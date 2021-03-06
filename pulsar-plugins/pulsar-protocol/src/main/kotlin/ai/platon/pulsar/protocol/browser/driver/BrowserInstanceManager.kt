package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.chrome.ChromeDevtoolsOptions
import ai.platon.pulsar.browser.driver.chrome.LauncherConfig
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class BrowserInstanceManager: AutoCloseable {
    private val closed = AtomicBoolean()
    private val browserInstances = ConcurrentHashMap<Path, BrowserInstance>()

    @Synchronized
    fun launchIfAbsent(launcherConfig: LauncherConfig, launchOptions: ChromeDevtoolsOptions): BrowserInstance {
        return browserInstances.computeIfAbsent(launchOptions.userDataDir) {
            BrowserInstance(launcherConfig, launchOptions).apply { launch() }
        }
    }

    fun closeIfPresent(dataDir: Path) {
        browserInstances.remove(dataDir)?.close()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            doClose()
        }
    }

    private fun doClose() {
        kotlin.runCatching {
            val unSynchronized = browserInstances.values.toList()
            browserInstances.clear()
            unSynchronized.parallelStream().forEach { it.close() }
        }.onFailure {
            // kill -9
            it.printStackTrace()
        }
    }
}
