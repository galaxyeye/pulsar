package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.HttpHeaders.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.files.ext.export
import ai.platon.pulsar.common.proxy.NoProxyException
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.component.FetchComponent
import ai.platon.pulsar.crawl.fetch.BatchStat
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.crawl.protocol.Content
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.RetryScope
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import ai.platon.pulsar.persist.model.BrowserJsData
import ai.platon.pulsar.proxy.InternalProxyServer
import org.apache.commons.lang.IllegalClassException
import org.apache.commons.lang.StringUtils
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.FluentWait
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollInterval: Duration
) {
    constructor(config: ImmutableConfig) : this(
            config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(120)),
            // wait page ready using script, so it can not smaller than pageLoadTimeout
            config.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(60)),
            config.getInt(FETCH_SCROLL_DOWN_COUNT, 5),
            config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))
    )
}

enum class FlowSate {
    CONTINUE, BREAK;

    val isContinue get() = this == CONTINUE
}

class FetchTask(
        val batchId: Int,
        val taskId: Int,
        val priority: Int,
        val page: WebPage,
        val volatileConfig: VolatileConfig
) {
    var batchSize: Int = 1
    var batchTaskId = 0
    val stat: BatchStat? = null
    var deleteAllCookies: Boolean = false
    var closeBrowsers: Boolean = false
    var proxyEntry: ProxyEntry? = null
    var retries = 0

    lateinit var response: Response

    val url get() = page.url
    val domain get() = URLUtil.getDomainName(url)

    companion object {
        val NIL = FetchTask(0, 0, 0, WebPage.NIL, VolatileConfig.EMPTY)
    }
}

class FetchResult(
        val task: FetchTask,
        var response: Response
)

class BrowseResult(
        var response: Response? = null,
        var exception: Exception? = null,
        var driver: ManagedWebDriver? = null
)

class JsTask(
        val url: String,
        val driver: ManagedWebDriver,
        val driverConfig: DriverConfig
)

data class VisitResult(
        var protocolStatus: ProtocolStatus,
        var jsData: BrowserJsData? = null,
        var state: FlowSate = FlowSate.CONTINUE
)

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
open class SeleniumEngine(
        browserControl: BrowserControl,
        val driverPool: WebDriverPool,
        protected val ips: InternalProxyServer,
        protected val fetchTaskTracker: FetchTaskTracker,
        protected val metricsSystem: MetricsSystem,
        protected val immutableConfig: ImmutableConfig
) : Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(SeleniumEngine::class.java)!!

    private val libJs = browserControl.parseLibJs(false)
    private val clientJs = browserControl.parseJs(false)
    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, true)
    private var charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    private val defaultDriverConfig = DriverConfig(immutableConfig)
    private val browserContext = BrowserContext(driverPool, ips, immutableConfig)
    private val maxCookieView = 40
    private val closed = AtomicBoolean(false)
    private val isClosed get() = closed.get()

    init {
        instanceCount.incrementAndGet()
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "instanceCount", instanceCount,
                "charsetPattern", StringUtils.abbreviateMiddle(charsetPattern.toString(), "...", 200),
                "pageLoadTimeout", defaultDriverConfig.pageLoadTimeout,
                "scriptTimeout", defaultDriverConfig.scriptTimeout,
                "scrollDownCount", defaultDriverConfig.scrollDownCount,
                "scrollInterval", defaultDriverConfig.scrollInterval,
                "clientJsLength", clientJs.length,
                "driverPoolCapacity", driverPool.capacity
        )
    }

    @Throws(IllegalStateException::class)
    fun fetch(task: FetchTask): FetchResult {
        checkState()

        fetchTaskTracker.totalTaskCount.getAndIncrement()
        fetchTaskTracker.batchTaskCounters.computeIfAbsent(task.batchId) { AtomicInteger() }.incrementAndGet()
        val result = browserContext.run(task) { _, driver ->
            browseWith(task, driver)
        }

        val response = when {
            result.driver == null -> ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
            result.response != null -> result.response!!
            result.exception != null -> ForwardingResponse(task.url, ProtocolStatus.failed(result.exception))
            else -> ForwardingResponse(task.url, ProtocolStatus.STATUS_FAILED)
        }

        return FetchResult(task, response)
    }

    protected open fun browseWith(task: FetchTask, driver: ManagedWebDriver): BrowseResult {
        checkState()

        ++task.retries

        var response: Response? = null
        var exception: Exception? = null

        try {
            response = browseWithMinorExceptionsHandled(task, driver)
        } catch (e: NoProxyException) {
            log.warn("No proxy, request is canceled | {}", task.url)
            response = ForwardingResponse(task.url, ProtocolStatus.STATUS_CANCELED)
        } catch (e: org.openqa.selenium.NoSuchSessionException) {
            log.warn("Web driver is crashed - {}", StringUtil.simplifyException(e))

            driver.retire()
            exception = e
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
        } catch (e: org.openqa.selenium.WebDriverException) {
            // status = ProtocolStatus.STATUS_RETRY
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                log.warn("Web driver is disconnected - {}", StringUtil.simplifyException(e))
            } else {
                log.warn("Unexpected WebDriver exception", e)
                // The following exceptions are found
                // 1. org.openqa.selenium.WebDriverException: unknown error: Cannot read property 'forEach' of null
            }

            driver.retire()
            exception = e
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
        } catch (e: org.apache.http.conn.HttpHostConnectException) {
            log.warn("Web driver is disconnected - {}", StringUtil.simplifyException(e))

            driver.retire()
            exception = e
            response = ForwardingResponse(task.url, ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL))
        } finally {
            trackProxy(driver.proxyEntry.get(), task, response)
        }

        return BrowseResult(response, exception, driver)
    }

    private fun browseWithMinorExceptionsHandled(task: FetchTask, driver: ManagedWebDriver): Response {
        checkState()

        val startTime = System.currentTimeMillis()

        val batchId = task.batchId
        val page = task.page

        val driverConfig = getDriverConfig(task.volatileConfig)
        val headers = MultiMetadata(Q_REQUEST_TIME, startTime.toString())

        var status: ProtocolStatus
        var pageSource = ""

        try {
            val result = navigateAndInteract(task, driver, driverConfig)
            status = result.protocolStatus
            page.browserJsData = result.jsData
            pageSource = driver.pageSource
        } catch (e: org.openqa.selenium.ScriptTimeoutException) {
            // ignore script timeout, document might lost data, but it's the page extractor's responsibility
            status = ProtocolStatus.STATUS_SUCCESS
        } catch (e: org.openqa.selenium.UnhandledAlertException) {
            // TODO: review the status, what's the proper way to handle this exception?
            log.warn(StringUtil.simplifyException(e))
            status = ProtocolStatus.STATUS_SUCCESS
        } catch (e: org.openqa.selenium.TimeoutException) {
            // TODO: which kind of timeout? resource loading timeout? script execution timeout? or web driver connection timeout?
            log.warn("Unexpected web driver timeout - {}", StringUtil.simplifyException(e))
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // TODO: when this exception is thrown?
            log.warn(e.message)
            status = ProtocolStatus.retry(RetryScope.FETCH_PROTOCOL)
        } catch (e: ai.platon.pulsar.net.browser.ContextResetException) {
            status = ProtocolStatus.retry(RetryScope.BROWSER_CONTEXT)
        }

        // Check quality of the page source, throw an exception if content is incomplete
        var code = 0
        if (pageSource.isNotEmpty()) {
            code = checkPageSource(pageSource, page, status, task)
        }

        // Check browse timeout event, transform status to be success if the page source is good
        val timeoutStatus = arrayOf(
                ProtocolStatusCodes.WEB_DRIVER_TIMEOUT,
                ProtocolStatusCodes.DOM_TIMEOUT
        )
        if (status.minorCode in timeoutStatus) {
            status = handleBrowseTimeout(startTime, pageSource, status, page, driverConfig)
        }

        // Update page source, modify charset directive, do the caching stuff
        if (code == 0) {
            pageSource = handlePageSource(pageSource).toString()
        }
        headers.put(CONTENT_LENGTH, pageSource.length.toString())

        if (code != 0) {
            status = ProtocolStatus.retry(RetryScope.BROWSER_CONTEXT)
        }

        // Update headers, metadata, do the logging stuff
        page.lastBrowser = driver.browserType
        handleBrowseFinish(page, headers)
        if (status.isSuccess) {
            handleBrowseSuccess(batchId)
        }

        exportIfNecessary(pageSource, status, page, driver)

        // TODO: collect response header
        // TODO: fetch only the major pages, css, js, etc, ignore the rest resources, ignore external resources

        val response = ForwardingResponse(page.url, pageSource, status, headers)
        // Eager update required page status for callbacks
        eagerUpdateWebPage(page, response, immutableConfig)
        return response
    }

    @Throws(ContextResetException::class, IllegalStateException::class, IllegalClassException::class, WebDriverException::class)
    private fun navigateAndInteract(task: FetchTask, driver: ManagedWebDriver, driverConfig: DriverConfig): VisitResult {
        checkState()

        val taskId = task.taskId
        val url = task.url
        val page = task.page

        if (log.isTraceEnabled) {
            log.trace("Navigate {}/{} in thd{}, drivers: {}/{}/{}(w/f/t) | {} | timeouts: {}/{}/{}",
                    taskId, task.batchSize,
                    Thread.currentThread().id,
                    driverPool.workingSize, driverPool.freeSize, driverPool.totalSize,
                    page.configuredUrl,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
            )
        }

        val timeouts = driver.driver.manage().timeouts()
        timeouts.pageLoadTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
        timeouts.setScriptTimeout(driverConfig.scriptTimeout.seconds, TimeUnit.SECONDS)

        checkContextState(driver)

        // TODO: handle frames
        // driver.switchTo().frame(1);

        driver.get(url)

        // Block and wait for the document is ready: all css and resources are OK
        val jsTask = JsTask(url, driver, driverConfig)
        return executeJs(jsTask)
    }

    @Throws(ContextResetException::class, IllegalStateException::class, IllegalClassException::class, WebDriverException::class)
    protected open fun executeJs(jsTask: JsTask): VisitResult {
        val result = VisitResult(ProtocolStatus.STATUS_SUCCESS, null)

        runJsAction(jsTask, result) { jsCheckDOMState(jsTask, result) }

        if (result.state.isContinue) {
            runJsAction(jsTask, result) { jsScrollDown(jsTask, result) }
        }

        if (result.state.isContinue) {
            runJsAction(jsTask, result) { jsComputeFeature(jsTask, result) }
        }

        return result
    }

    @Throws(ContextResetException::class, IllegalStateException::class, IllegalClassException::class, WebDriverException::class)
    private fun runJsAction(task: JsTask, result: VisitResult, action: () -> Unit) {
        checkState()

        var status: ProtocolStatus? = null

        try {
            action()
            result.state = FlowSate.CONTINUE
        } catch (e: InterruptedException) {
            log.warn("Interrupted waiting for document | {}", task.url)
            status = ProtocolStatus.STATUS_CANCELED
            result.state = FlowSate.BREAK
        } catch (e: WebDriverException) {
            val message = StringUtil.stringifyException(e)
            when {
                e.cause is org.apache.http.conn.HttpHostConnectException -> {
                    // Web driver closed
                    // status = ProtocolStatus.failed(ProtocolStatus.WEB_DRIVER_GONE, e)
                    throw e
                }
                e.cause is InterruptedException -> {
                    // Web driver closed
                    if (message.contains("sleep interrupted")) {
                        log.warn("Interrupted waiting for DOM, sleep interrupted | {}", task.url)
                        status = ProtocolStatus.retry(RetryScope.CRAWL_SOLUTION)
                    } else {
                        log.warn("Interrupted waiting for DOM | {} \n>>>\n{}\n<<<", task.url, StringUtil.stringifyException(e))
                    }
                    result.state = FlowSate.BREAK
                }
                message.contains("Cannot read property") -> {
                    // unknown error: Cannot read property 'forEach' of null
                    log.warn("Javascript exception | {} {}", task.url, StringUtil.simplifyException(e))
                    // ignore script errors, document might lost data, but it's the page extractor's responsibility
                    status = ProtocolStatus.STATUS_SUCCESS
                    result.state = FlowSate.CONTINUE
                }
                else -> {
                    log.warn("Unexpected WebDriver exception | {} \n>>>\n{}\n<<<", task.url, StringUtil.stringifyException(e))
                    throw e
                }
            }
        }

        if (status != null) {
            result.protocolStatus = status
        }
    }

    protected open fun jsCheckDOMState(jsTask: JsTask, result: VisitResult) {
        checkState()

        var status = ProtocolStatus.STATUS_SUCCESS
        // wait for the DOM ready, we do not use scriptTimeout here
        val pageLoadTimeout = jsTask.driverConfig.pageLoadTimeout

        val documentWait = FluentWait<WebDriver>(jsTask.driver.driver)
                .withTimeout(pageLoadTimeout.seconds, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS)
                .ignoring(InterruptedException::class.java)

        // make sure the document is ready
        val initialScroll = 2
        val maxRound = pageLoadTimeout.seconds - 10 // leave 10 seconds to wait for script finish

        // TODO: wait for expected ni, na, nnum, nst, etc; required element
        val js = ";$libJs;return __utils__.waitForReady($maxRound, $initialScroll);"

        checkContextState(jsTask.driver)

        try {
            val r = documentWait.until { jsTask.driver.executeScript(js) }

            if (r == "timeout") {
                log.debug("Hit max round $maxRound to wait for document | {}", jsTask.url)
            } else {
                log.trace("DOM is ready {} | {}", r, jsTask.url)
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            log.trace("DOM is timeout ({}) | {}", pageLoadTimeout, jsTask.url)
            status = ProtocolStatus.failed(ProtocolStatusCodes.DOM_TIMEOUT)
        }

        result.protocolStatus = status
    }

    protected open fun jsScrollDown(jsTask: JsTask, result: VisitResult) {
        checkState()

        val scrollDownCount = jsTask.driverConfig.scrollDownCount.toLong()
        val scrollInterval = jsTask.driverConfig.scrollInterval
        val timeout = scrollDownCount * scrollInterval.toMillis() + 3 * 1000
        val scrollWait = FluentWait<WebDriver>(jsTask.driver.driver)
                .withTimeout(timeout, TimeUnit.MILLISECONDS)
                .pollingEvery(scrollInterval.toMillis(), TimeUnit.MILLISECONDS)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            val js = ";$libJs;return __utils__.scrollDownN($scrollDownCount);"

            checkContextState(jsTask.driver)
            scrollWait.until { jsTask.driver.executeScript(js) }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }
    }

    protected open fun jsComputeFeature(jsTask: JsTask, result: VisitResult) {
        checkState()

        // TODO: check if the js is injected times, libJs is already injected
        checkContextState(jsTask.driver)
        val message = jsTask.driver.executeScript(clientJs)

        if (message is String) {
            val jsData = BrowserJsData.fromJson(message)
            if (log.isDebugEnabled) {
                log.debug("{} | {}", jsData, jsTask.url)
            }
            result.jsData = jsData
        }
    }

    /**
     * Perform click on the selected element and wait for the new page location
     * */
    protected open fun jsClick(jsExecutor: JavascriptExecutor, selector: String, driver: ManagedWebDriver, driverConfig: DriverConfig): String {
        checkState()

        val timeout = driverConfig.pageLoadTimeout
        val scrollWait = FluentWait<WebDriver>(driver.driver)
                .withTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .pollingEvery(1000, TimeUnit.MILLISECONDS)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            // TODO: which one is the better? browser side timer or selenium side timer?
            val js = ";$libJs;return __utils__.navigateTo($selector);"
            checkContextState(driver)
            val location = scrollWait.until { (it as? JavascriptExecutor)?.executeScript(js) }
            if (location is String) {
                return location
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }

        return ""
    }

    protected open fun checkPageSource(pageSource: String, page: WebPage, status: ProtocolStatus, task: FetchTask): Int {
        return 0
    }

    protected fun handleIncompleteContent(task: FetchTask, message: String) {
        val proxyEntry = task.proxyEntry
        val domain = task.domain
        val link = AppPaths.symbolicLinkFromUri(task.url)

        if (proxyEntry != null) {
            val count = proxyEntry.servedDomains.count(domain)
            log.warn("INCOMPLETE - domain: {}({}) proxy: {} - {} | {}",
                    domain, count, proxyEntry.display, message, link)
        } else {
            log.warn("INCOMPLETE - {} | {}", message, link)
        }
    }

    protected open fun handleBrowseTimeout(startTime: Long, pageSource: String, status: ProtocolStatus, page: WebPage, driverConfig: DriverConfig): ProtocolStatus {
        logBrowseTimeout(page.url, startTime, pageSource, driverConfig)
        return status
    }

    protected open fun handleBrowseFinish(page: WebPage, headers: MultiMetadata) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(CONTENT_ENCODING, "UTF-8")
        headers.put(Q_TRUSTED_CONTENT_ENCODING, "UTF-8")
        headers.put(Q_RESPONSE_TIME, System.currentTimeMillis().toString())

        val urls = page.browserJsData?.urls
        if (urls != null) {
            page.location = urls.location
            if (page.url != page.location) {
                // in-browser redirection
                metricsSystem.debugRedirects(page.url, urls)
            }
        }
    }

    protected open fun handleBrowseSuccess(batchId: Int) {
        val t = fetchTaskTracker
        t.batchSuccessCounters.computeIfAbsent(batchId) { AtomicInteger() }.incrementAndGet()
        t.totalSuccessCount.incrementAndGet()

        // TODO: A metrics system is required
        if (t.totalTaskCount.get() % 20 == 0) {
            log.debug("Selenium task success: {}/{}, total task success: {}/{}",
                    t.batchSuccessCounters[batchId], t.batchTaskCounters[batchId],
                    t.totalSuccessCount,
                    t.totalTaskCount
            )
        }
    }

    protected open fun handlePageSource(pageSource: String): StringBuilder {
        return replaceHTMLCharset(pageSource, charsetPattern)
    }

    private fun exportIfNecessary(pageSource: String, status: ProtocolStatus, page: WebPage, driver: ManagedWebDriver) {
        if (log.isDebugEnabled && pageSource.isNotEmpty()) {
            val path = AppFiles.export(status, pageSource, page)

            // Create symbolic link with an url based, unique, shorter but not readable file name,
            // we can generate and refer to this path at any place
            val link = AppPaths.symbolicLinkFromUri(page.url)
            Files.deleteIfExists(link)
            Files.createSymbolicLink(link, path)

            if (log.isTraceEnabled) {
                takeScreenshot(pageSource.length.toLong(), page, driver.driver as RemoteWebDriver)
            }
        }
    }

    protected open fun logBrowseTimeout(url: String, startTime: Long, pageSource: String, driverConfig: DriverConfig) {
        val elapsed = Duration.ofMillis(System.currentTimeMillis() - startTime)
        if (log.isDebugEnabled) {
            val link = AppPaths.symbolicLinkFromUri(url)
            log.debug("Timeout after {} with {} drivers: {}/{}/{} timeouts: {}/{}/{} | file://{}",
                    elapsed, StringUtil.readableByteCount(pageSource.length.toLong()),
                    driverPool.workingSize, driverPool.freeSize, driverPool.totalSize,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
                    link)
        }
    }

    /**
     * Eager update web page, the status is incomplete but required by callbacks
     * */
    private fun eagerUpdateWebPage(page: WebPage, response: Response, conf: ImmutableConfig) {
        page.protocolStatus = response.status
        val bytes = response.content
        val contentType = response.getHeader(HttpHeaders.CONTENT_TYPE)
        val content = Content(page.url, page.location, bytes, contentType, response.headers, conf)
        FetchComponent.updateContent(page, content)
    }

    private fun getDriverConfig(config: ImmutableConfig): DriverConfig {
        // Page load timeout
        val pageLoadTimeout = config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, defaultDriverConfig.pageLoadTimeout)
        // Script timeout
        val scriptTimeout = config.getDuration(FETCH_SCRIPT_TIMEOUT, defaultDriverConfig.scriptTimeout)
        // Scrolling
        var scrollDownCount = config.getInt(FETCH_SCROLL_DOWN_COUNT, defaultDriverConfig.scrollDownCount)
        if (scrollDownCount > 20) {
            scrollDownCount = 20
        }
        var scrollDownWait = config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, defaultDriverConfig.scrollInterval)
        if (scrollDownWait > pageLoadTimeout) {
            scrollDownWait = pageLoadTimeout
        }

        // TODO: handle proxy

        return DriverConfig(pageLoadTimeout, scriptTimeout, scrollDownCount, scrollDownWait)
    }

    private fun takeScreenshot(contentLength: Long, page: WebPage, driver: RemoteWebDriver) {
        if (RemoteWebDriver::class.java.isAssignableFrom(driver.javaClass)) {
            try {
                if (contentLength > 100) {
                    val bytes = driver.getScreenshotAs(OutputType.BYTES)
                    AppFiles.export(page, bytes, ".png")
                }
            } catch (e: Exception) {
                log.warn("Screenshot failed {} | {}", StringUtil.readableByteCount(contentLength), page.url)
            }
        }
    }

    /**
     * Check if context reset occurs
     * every javascript execution is a checkpoint for context reset
     * */
    @Throws(ContextResetException::class, IllegalStateException::class)
    private fun checkContextState(driver: ManagedWebDriver) {
        if (isClosed) {
            throw IllegalStateException("Selenium engine is closed")
        }

        if (driver.isPaused) {
            throw ContextResetException()
        }
    }

    @Throws(IllegalStateException::class)
    private fun checkState() {
        if (isClosed) {
            throw IllegalStateException("Selenium engine is closed")
        }
    }

    private fun logBrowseDone(retryRound: Int, task: FetchTask, result: BrowseResult) {
        if (log.isInfoEnabled) {
            val r = result.response
            if (retryRound > 1 && r != null && r.status.isSuccess && r.length() > 100_1000) {
                log.info("Retried {} times and obtain a good page with {} | {}",
                        retryRound, StringUtil.readableByteCount(r.length()), task.url)
            }
        }
    }

    private fun trackProxy(proxyEntry: ProxyEntry?, task: FetchTask, response: Response?) {
        task.proxyEntry = proxyEntry
        if (proxyEntry != null && response != null && response.status.isSuccess) {
            task.page.metadata.set(Name.PROXY, proxyEntry.hostPort)
            proxyEntry.servedDomains.add(task.domain)
            proxyEntry.targetHost = task.page.url
        }
    }

    override fun close() {
        if (closed.getAndSet(true)) {
            return
        }
    }

    companion object {
        private var instanceCount = AtomicInteger()
    }
}
