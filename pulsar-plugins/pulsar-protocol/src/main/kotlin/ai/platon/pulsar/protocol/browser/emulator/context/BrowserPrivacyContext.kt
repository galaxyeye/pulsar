package ai.platon.pulsar.protocol.browser.emulator.context

import ai.platon.pulsar.common.PulsarParams.VAR_PRIVACY_CONTEXT_NAME
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.proxy.*
import ai.platon.pulsar.common.readable
import ai.platon.pulsar.crawl.fetch.CoreMetrics
import ai.platon.pulsar.crawl.fetch.FetchResult
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContext
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyContextId
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import org.slf4j.LoggerFactory

/**
 * The privacy context, the context is closed if privacy is leaked
 * */
open class BrowserPrivacyContext(
    val proxyPoolManager: ProxyPoolManager? = null,
    val driverPoolManager: WebDriverPoolManager,
    val coreMetrics: CoreMetrics? = null,
    conf: ImmutableConfig,
    id: PrivacyContextId
): PrivacyContext(id, conf) {
    private val log = LoggerFactory.getLogger(BrowserPrivacyContext::class.java)
    private val browserInstanceId = BrowserInstanceId.resolve(id.dataDir)
    private val driverContext = WebDriverContext(browserInstanceId, driverPoolManager, conf)
    private var proxyContext: ProxyContext? = null
    var proxyEntry: ProxyEntry? = null
    val numFreeDrivers get() = driverPoolManager.numFreeDrivers
    val numWorkingDrivers get() = driverPoolManager.numWorkingDrivers
    val numAvailableDrivers get() = driverPoolManager.numAvailableDrivers

    @Throws(NoProxyException::class, ProxyVendorUntrustedException::class)
    override suspend fun doRun(task: FetchTask, browseFun: suspend (FetchTask, WebDriver) -> FetchResult): FetchResult {
        initialize(task)

        return checkAbnormalResult(task) ?:
            proxyContext?.run(task, browseFun) ?:
            driverContext.run(task, browseFun)
    }

    override fun report() {
        val isIdle = proxyContext?.proxyEntry?.isIdle == true
        log.info("Privacy context #{}{}{} has lived for {}" +
                " | success: {}({} pages/s) | small: {}({}) | traffic: {}({}/s) | tasks: {} total run: {} | {}",
                display, if (isIdle) "(idle)" else "", if (isLeaked) "(leaked)" else "", elapsedTime.readable(),
                meterSuccesses.count, String.format("%.2f", meterSuccesses.meanRate),
                meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate),
                Strings.readableBytes(coreMetrics?.totalNetworkIFsRecvBytes?:0),
                Strings.readableBytes(coreMetrics?.networkIFsRecvBytesPerSecond?:0),
                meterTasks.count, meterFinishes.count,
                proxyContext?.proxyEntry
        )

        if (smallPageRate > 0.5) {
            log.warn("Privacy context #{} is disqualified, too many small pages: {}({})",
                    sequence, meterSmallPages.count, String.format("%.1f%%", 100 * smallPageRate))
        }

        // 0 to disable
        if (meterSuccesses.meanRate < 0) {
            log.warn("Privacy context #{} is disqualified, it's expected 120 pages in 120 seconds at least", sequence)
            // check the zombie context list, if the context keeps go bad, the proxy provider is bad
        }
    }

    /**
     * Block until all the drivers are closed and the proxy is offline
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            report()
            driverContext.shutdown()
            proxyContext?.close()
        }
    }

    private fun checkAbnormalResult(task: FetchTask): FetchResult? {
        return when {
            !isActive -> FetchResult.privacyRetry(task)
            else -> null
        }
    }

    @Throws(ProxyException::class)
    @Synchronized
    private fun initialize(task: FetchTask) {
        if (proxyEntry == null && proxyPoolManager != null && proxyPoolManager.isEnabled) {
            val pc = ProxyContext.create(id, driverContext, proxyPoolManager, conf)
            proxyEntry = pc.proxyEntry
            browserInstanceId.proxyServer = proxyEntry?.hostPort
            proxyContext = pc
        }
        task.page.variables[VAR_PRIVACY_CONTEXT_NAME] = display
    }
}
