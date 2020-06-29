package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.DEFAULT_CHARSET_PATTERN
import ai.platon.pulsar.common.IllegalApplicationContextStateException
import ai.platon.pulsar.common.SYSTEM_AVAILABLE_CHARSET_PATTERN
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.common.message.MiscMessageWriter
import ai.platon.pulsar.common.prependReadableClassName
import ai.platon.pulsar.crawl.fetch.FetchTask
import ai.platon.pulsar.protocol.browser.driver.ManagedWebDriver
import ai.platon.pulsar.protocol.browser.emulator.context.BrowserPrivacyManager
import com.codahale.metrics.SharedMetricRegistries
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

abstract class BrowserEmulatorBase(
        val privacyManager: BrowserPrivacyManager,
        val eventHandlerFactory: BrowserEmulatorEventHandlerFactory,
        val messageWriter: MiscMessageWriter,
        val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    private val log = LoggerFactory.getLogger(BrowserEmulatorBase::class.java)!!
    private val tracer = log.takeIf { it.isTraceEnabled }
    val eventHandler = eventHandlerFactory.eventHandler
    val supportAllCharsets get() = immutableConfig.getBoolean(CapabilityTypes.PARSE_SUPPORT_ALL_CHARSETS, true)
    val charsetPattern = if (supportAllCharsets) SYSTEM_AVAILABLE_CHARSET_PATTERN else DEFAULT_CHARSET_PATTERN
    val fetchMaxRetry = immutableConfig.getInt(CapabilityTypes.HTTP_FETCH_MAX_RETRY, 3)
    val closed = AtomicBoolean(false)
    val isClosed get() = closed.get()
    val isActive get() = !closed.get()
    val driverManager = privacyManager.driverManager
    val driverControl = driverManager.driverControl
    val metrics = SharedMetricRegistries.getDefault()
    val meterNavigates = metrics.meter(prependReadableClassName(this,"navigates"))
    val counterRequests = metrics.counter(prependReadableClassName(this,"requests"))
    val counterCancels = metrics.counter(prependReadableClassName(this,"cancels"))

    var enableDelayBeforeNavigation = false
    var lastNavigateTime = Instant.EPOCH

    override fun getParams(): Params {
        return Params.of(
                "charsetPattern", StringUtils.abbreviateMiddle(charsetPattern.toString(), "...", 200),
                "pageLoadTimeout", driverControl.pageLoadTimeout,
                "scriptTimeout", driverControl.scriptTimeout,
                "scrollDownCount", driverControl.scrollDownCount,
                "scrollInterval", driverControl.scrollInterval,
                "jsInvadingEnabled", driverControl.jsInvadingEnabled,
                "poolMonitor", immutableConfig.get(CapabilityTypes.PROXY_POOL_MONITOR_CLASS),
                "eventHandler", immutableConfig.get(CapabilityTypes.BROWSER_EMULATE_EVENT_HANDLER)
        )
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    @Throws(IllegalApplicationContextStateException::class)
    protected fun checkState() {
        if (!isActive) {
            throw IllegalApplicationContextStateException("Emulator is closed")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected fun checkState(driver: ManagedWebDriver) {
        checkState()

        if (driver.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw NavigateTaskCancellationException("Task with driver #${driver.id} is canceled | ${driver.url}")
        }
    }

    /**
     * Check task state
     * every direct or indirect IO operation is a checkpoint for the context reset event
     * */
    @Throws(NavigateTaskCancellationException::class, IllegalApplicationContextStateException::class)
    protected fun checkState(task: FetchTask) {
        checkState()

        if (task.isCanceled) {
            // the task is canceled, so the navigation is stopped, the driver is closed, the privacy context is reset
            // and all the running tasks should be redo
            throw NavigateTaskCancellationException("Task #${task.batchTaskId}/${task.batchId} is canceled | ${task.url}")
        }
    }
}
