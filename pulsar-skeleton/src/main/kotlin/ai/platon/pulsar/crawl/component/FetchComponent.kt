/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.crawl.component

import ai.platon.pulsar.common.PulsarParams.VAR_LOAD_OPTIONS
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.common.urls.NormUrl
import ai.platon.pulsar.crawl.common.URLUtil
import ai.platon.pulsar.crawl.fetch.CoreMetrics
import ai.platon.pulsar.persist.PageDatum
import ai.platon.pulsar.crawl.protocol.ProtocolFactory
import ai.platon.pulsar.crawl.protocol.ProtocolNotFound
import ai.platon.pulsar.crawl.protocol.ProtocolOutput
import ai.platon.pulsar.persist.CrawlStatus
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.Mark
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

class FetchEntry(val page: WebPage, val options: LoadOptions) {

    constructor(url: String, options: LoadOptions, href: String? = null) :
            this(createPageShell(url, options, href), options)

    companion object {

        fun createPageShell(normUrl: NormUrl): WebPage {
            return createPageShell(normUrl.spec, normUrl.options, normUrl.hrefSpec)
        }

        fun createPageShell(url: String, options: LoadOptions, href: String? = null): WebPage {
            val page = WebPage.newWebPage(url, options.conf, href)

            initWebPage(page, options, href)

            return page
        }

        fun initWebPage(page: WebPage, options: LoadOptions, href: String? = null) {
            page.also {
                it.href = href
                it.fetchMode = options.fetchMode
                it.conf = options.conf
                it.args = options.toString()

                it.variables[VAR_LOAD_OPTIONS] = options
            }
        }
    }
}

/**
 * Created by vincent on 17-5-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * Fetch component
 */
@Component
open class FetchComponent(
    val coreMetrics: CoreMetrics? = null,
    val protocolFactory: ProtocolFactory,
    val immutableConfig: ImmutableConfig,
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(FetchComponent::class.java)
    private val tracer = logger.takeIf { it.isTraceEnabled }

    private val closed = AtomicBoolean()
    val isActive get() = !closed.get()
    private val abnormalPage get() = WebPage.NIL.takeIf { !isActive }

    /**
     * Fetch an url
     *
     * @param url The url of web page to fetch
     * @return The fetch result
     */
    fun fetch(url: String) = abnormalPage ?: fetchContent(WebPage.newWebPage(url, immutableConfig.toVolatileConfig()))

    /**
     * Fetch an url
     *
     * @param url The url of web page to fetch
     * @param options The options
     * @return The fetch result
     */
    fun fetch(url: String, options: LoadOptions) = abnormalPage ?: fetchContent0(FetchEntry(url, options))

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    fun fetchContent(page: WebPage) = abnormalPage ?: fetchContent0(FetchEntry(page, page.options))

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    fun fetchContent(fetchEntry: FetchEntry) = abnormalPage ?: fetchContent0(fetchEntry)

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    suspend fun fetchContentDeferred(page: WebPage) = abnormalPage ?: fetchContentDeferred0(page)

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    protected fun fetchContent0(fetchEntry: FetchEntry): WebPage {
        val page = fetchEntry.page
        require(page.isNotInternal) { "Internal page ${page.url}" }

        return try {
            beforeFetch(page)

            coreMetrics?.markTaskStart()
            val protocol = protocolFactory.getProtocol(page)
            processProtocolOutput(page, protocol.getProtocolOutput(page))
        } catch (e: ProtocolNotFound) {
            logger.warn(e.message)
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND, CrawlStatus.STATUS_UNFETCHED) }
        } finally {
            afterFetch(page)
        }
    }

    /**
     * Fetch a page
     *
     * @param page The page to fetch
     * @return The fetch result
     */
    protected suspend fun fetchContentDeferred0(page: WebPage): WebPage {
        return try {
            beforeFetch(page)

            coreMetrics?.markTaskStart()
            val protocol = protocolFactory.getProtocol(page)
            processProtocolOutput(page, protocol.getProtocolOutputDeferred(page))
        } catch (e: ProtocolNotFound) {
            logger.warn(e.message)
            page.also { updateStatus(it, ProtocolStatus.STATUS_PROTO_NOT_FOUND, CrawlStatus.STATUS_UNFETCHED) }
        } finally {
            afterFetch(page)
        }
    }

    private fun beforeFetch(page: WebPage) {
        try {
            page.loadEventHandler?.onBeforeFetch?.invoke(page)
        } catch (e: Throwable) {
            log.warn("Failed to invoke beforeFetch | ${page.configuredUrl}", e)
        }
    }

    private fun afterFetch(page: WebPage) {
        try {
            page.loadEventHandler?.onAfterFetch?.invoke(page)
        } catch (e: Throwable) {
            log.warn("Failed to invoke afterFetch | ${page.configuredUrl}", e)
        }
    }

    protected fun processProtocolOutput(page: WebPage, output: ProtocolOutput): WebPage {
        val url = page.url
        val pageDatum = output.pageDatum
        if (pageDatum == null) {
            logger.warn("No content | {}", page.configuredUrl)
        }

        page.isFetched = true

        page.headers.putAll(output.headers.asMultimap())
        val protocolStatus = output.protocolStatus

        val crawlStatus = when (protocolStatus.minorCode) {
            ProtocolStatus.SUCCESS_OK -> CrawlStatus.STATUS_FETCHED
            ProtocolStatus.NOT_MODIFIED -> CrawlStatus.STATUS_NOTMODIFIED
            ProtocolStatus.CANCELED -> CrawlStatus.STATUS_UNFETCHED

            ProtocolStatus.MOVED_PERMANENTLY,
            ProtocolStatus.MOVED_TEMPORARILY,
            -> handleMoved(page, protocolStatus).also { coreMetrics?.trackMoved(url) }

            ProtocolStatus.UNAUTHORIZED,
            ProtocolStatus.ROBOTS_DENIED,
            ProtocolStatus.UNKNOWN_HOST,
            ProtocolStatus.GONE,
            ProtocolStatus.NOT_FOUND,
            -> CrawlStatus.STATUS_GONE.also { coreMetrics?.trackHostUnreachable(url) }

            ProtocolStatus.EXCEPTION,
            ProtocolStatus.RETRY,
            ProtocolStatus.BLOCKED,
            -> CrawlStatus.STATUS_RETRY.also { coreMetrics?.trackHostUnreachable(url) }

            ProtocolStatus.REQUEST_TIMEOUT,
            ProtocolStatus.THREAD_TIMEOUT,
            ProtocolStatus.WEB_DRIVER_TIMEOUT,
            ProtocolStatus.SCRIPT_TIMEOUT,
            -> CrawlStatus.STATUS_RETRY.also { coreMetrics?.trackTimeout(url) }

            else -> CrawlStatus.STATUS_RETRY.also { logger.warn("Unknown protocol status $protocolStatus") }
        }

        when (crawlStatus) {
            CrawlStatus.STATUS_FETCHED,
            CrawlStatus.STATUS_REDIR_TEMP,
            CrawlStatus.STATUS_REDIR_PERM,
            -> updateFetchedPage(page, pageDatum, protocolStatus, crawlStatus)
            else -> updateFetchedPage(page, null, protocolStatus, crawlStatus)
        }

        if (crawlStatus.isFetched) {
            coreMetrics?.trackSuccess(page)
        } else if (crawlStatus.isFailed) {
            coreMetrics?.trackFailedUrl(url)
        }

        return page
    }

    private fun handleMoved(page: WebPage, protocolStatus: ProtocolStatus): CrawlStatus {
        val crawlStatus: CrawlStatus
        val url = page.url
        val minorCode = protocolStatus.minorCode
        val temp: Boolean
        if (minorCode == ProtocolStatus.MOVED_PERMANENTLY) {
            crawlStatus = CrawlStatus.STATUS_REDIR_PERM
            temp = false
        } else {
            crawlStatus = CrawlStatus.STATUS_REDIR_TEMP
            temp = true
        }

        val newUrl = protocolStatus.getArgOrDefault(ProtocolStatus.ARG_REDIRECT_TO_URL, "")
        if (newUrl.isNotEmpty()) {
            // handleRedirect(url, newUrl, temp, PROTOCOL_REDIR, fetchTask.getPage());
            val reprUrl = URLUtil.chooseRepr(url, newUrl, temp)
            if (reprUrl.length >= AppConstants.SHORTEST_VALID_URL_LENGTH) {
                page.reprUrl = reprUrl
            }
        }
        return crawlStatus
    }

    /**
     * TODO: do this in update phrase
     * */
    private fun updateFetchedPage(
        page: WebPage, pageDatum: PageDatum?,
        protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus,
    ): WebPage {
        updateStatus(page, protocolStatus, crawlStatus)

        pageDatum?.also {
            page.location = it.location
            page.proxy = it.proxyEntry?.outIp
            val ms = it.activeDomMultiStatus
            if (ms != null) {
                page.activeDomStatus = ms.status
                page.activeDomStats = mapOf(
                    "initStat" to ms.initStat, "initD" to ms.initD,
                    "lastStat" to ms.lastStat, "lastD" to ms.lastD
                )
            }

            it.activeDomUrls?.let { page.activeDomUrls = it }
            it.pageCategory?.let { page.setPageCategory(it) }
            it.htmlIntegrity?.let { page.htmlIntegrity = it }
            it.lastBrowser?.let { page.lastBrowser = it }

            if (protocolStatus.isSuccess) {
                updateContent(page, it)
            }
        }

        updateMarks(page)

        return page
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FetchComponent::class.java)

        fun updateStatus(page: WebPage, protocolStatus: ProtocolStatus, crawlStatus: CrawlStatus) {
            page.crawlStatus = crawlStatus
            page.protocolStatus = protocolStatus
            page.updateFetchCount()
        }

        fun updateMarks(page: WebPage) {
            val marks = page.marks
            marks.putIfNotNull(Mark.FETCH, marks[Mark.GENERATE])
        }

        fun updateContent(page: WebPage, pageDatum: PageDatum, contentTypeHint: String? = null) {
            var contentType = contentTypeHint

            page.setContent(pageDatum.content)

            if (contentType != null) {
                pageDatum.contentType = contentType
            } else {
                contentType = pageDatum.contentType
            }

            if (contentType != null) {
                page.contentType = contentType
            } else {
                log.warn("Failed to determine content type!")
            }
        }
    }
}
