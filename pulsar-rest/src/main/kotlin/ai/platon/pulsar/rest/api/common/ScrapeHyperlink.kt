package ai.platon.pulsar.rest.api.common

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.PulsarParams.VAR_IS_SCRAPE
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.Strings
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.persist.ext.loadEventHandler
import ai.platon.pulsar.crawl.DefaultLoadEventHandler
import ai.platon.pulsar.crawl.LoadEventPipelineHandler
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.url.CompletableListenableHyperlink
import ai.platon.pulsar.crawl.common.url.StatefulListenableHyperlink
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.context.AbstractSQLContext
import ai.platon.pulsar.ql.ResultSets
import ai.platon.pulsar.ql.h2.utils.ResultSetUtils
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeResponse
import org.h2.jdbc.JdbcSQLException
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class ScrapeLoadEventHandler(
    val hyperlink: ScrapeHyperlink,
    val response: ScrapeResponse,
) : DefaultLoadEventHandler() {
    init {
        onBeforeLoadPipeline.addLast {
            response.pageStatusCode = ResourceStatus.SC_PROCESSING
        }
        onBeforeParsePipeline.addLast { page ->
            require(page.loadEventHandler === this)
            page.variables[VAR_IS_SCRAPE] = true
        }
        onBeforeHtmlParsePipeline.addLast { page ->
        }
        onAfterHtmlParsePipeline.addLast { page, document ->
            require(page.loadEventHandler === this)
            require(page.hasVar(VAR_IS_SCRAPE))
            hyperlink.extract(page, document)
        }
        onAfterLoadPipeline.addLast { page ->
            require(page.loadEventHandler === this)
            hyperlink.complete(page)
        }
    }
}

open class ScrapeHyperlink(
    val request: ScrapeRequest,
    val sql: NormXSQL,
    val session: PulsarSession,
    val globalCache: GlobalCache,
    val uuid: String = UUID.randomUUID().toString()
) : CompletableListenableHyperlink<ScrapeResponse>(sql.url) {

    private val logger = getLogger(ScrapeHyperlink::class)

    private val sqlContext get() = session.context as AbstractSQLContext
    private val connectionPool get() = sqlContext.connectionPool
    private val randomConnection get() = sqlContext.randomConnection

    val response = ScrapeResponse()

    override var args: String? = "-parse ${sql.args}"
    override val loadEventHandler: LoadEventPipelineHandler = ScrapeLoadEventHandler(this, response)

    open fun executeQuery(): ResultSet = executeQuery(request, response)

    open fun extract(page: WebPage, document: FeaturedDocument) {
        try {
            response.pageContentBytes = page.contentLength.toInt()
            response.pageStatusCode = page.protocolStatus.minorCode

            doExtract(page, document)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    open fun complete(page: WebPage) {
        response.isDone = true
        response.finishTime = Instant.now()

        complete(response)
    }

    protected open fun doExtract(page: WebPage, document: FeaturedDocument): ResultSet {
        if (!page.protocolStatus.isSuccess || page.contentLength == 0L || page.content == null) {
            response.statusCode = ResourceStatus.SC_NO_CONTENT
            return ResultSets.newSimpleResultSet()
        }

        return executeQuery(request, response)
    }

    protected open fun executeQuery(request: ScrapeRequest, response: ScrapeResponse): ResultSet {
        var rs: ResultSet = ResultSets.newSimpleResultSet()

        try {
            response.statusCode = ResourceStatus.SC_OK

            rs = executeQuery(sql.sql)
            val resultSet = mutableListOf<Map<String, Any?>>()
            ResultSetUtils.getEntitiesFromResultSetTo(rs, resultSet)
            response.resultSet = resultSet
        } catch (e: JdbcSQLException) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
            logger.warn("Failed to execute sql #${response.uuid}{}", Strings.simplifyException(e))
        } catch (e: Throwable) {
            response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
            logger.warn("Failed to execute sql #${response.uuid}\n{}", Strings.stringifyException(e))
        }

        return rs
    }

    private fun executeQuery(sql: String): ResultSet {
//        return session.executeQuery(sql)
        val connection = connectionPool.poll() ?: randomConnection
        return executeQuery(sql, connection).also { connectionPool.offer(connection) }
    }

    private fun executeQuery(sql: String, conn: Connection): ResultSet {
        var result: ResultSet? = null
        val millis = measureTimeMillis {
            conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)?.use { st ->
                try {
                    st.executeQuery(sql)?.use { rs ->
                        result = ResultSetUtils.copyResultSet(rs)
                    }
                } catch (e: JdbcSQLException) {
                    val message = e.toString()
                    if (message.contains("Syntax error in SQL statement")) {
                        response.statusCode = ResourceStatus.SC_BAD_REQUEST
                        logger.warn("Syntax error in SQL statement #${response.uuid}>>>\n{}\n<<<", e.sql)
                    } else {
                        response.statusCode = ResourceStatus.SC_EXPECTATION_FAILED
                        logger.warn("Failed to execute scrape task #${response.uuid}\n{}",
                            Strings.stringifyException(e))
                    }
                }
            }
        }

        return result ?: ResultSets.newResultSet()
    }
}
