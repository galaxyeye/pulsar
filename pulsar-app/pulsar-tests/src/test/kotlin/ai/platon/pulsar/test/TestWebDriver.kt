package ai.platon.pulsar.test

import ai.platon.pulsar.common.config.CapabilityTypes.PROXY_USE_PROXY
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import ai.platon.pulsar.protocol.browser.emulator.DefaultWebDriverPoolManager
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.openqa.selenium.remote.CapabilityType
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * TODO: move to pulsar-skeleton module
 * */
class TestWebDriver {
    companion object {
        val log = LoggerFactory.getLogger(TestWebDriver::class.java)

        init {
            System.setProperty(PROXY_USE_PROXY, "no")
        }

        val context = PulsarContexts.activate()
        val conf = context.unmodifiedConfig
        val driverControl = WebDriverControl(conf)
        val driverPoolManager = DefaultWebDriverPoolManager(conf)
        var quitMultiThreadTesting = false

        @BeforeClass
        fun setup() {

        }

        @AfterClass
        fun teardown() {
        }
    }

    @Test
    fun testCapabilities() {
        val generalOptions = driverControl.createGeneralOptions()
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)
        generalOptions.setCapability(CapabilityType.PROXY, null as Any?)

        val chromeOptions = driverControl.createChromeOptions()
        chromeOptions.addArguments("--blink-settings=imagesEnabled=false")
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
        chromeOptions.setCapability(CapabilityType.PROXY, null as Any?)
    }

    @Test
    fun testWebDriverPool() {
        val driverPool = driverPoolManager.createUnmanagedDriverPool()
        val workingDrivers = mutableListOf<WebDriver>()
        repeat(10) {
            val driver = driverPool.poll(conf.toVolatileConfig())
            workingDrivers.add(driver)
        }

        assertEquals(10, driverPool.numWorking.get())
        assertEquals(0, driverPool.numFree)
        assertEquals(10, driverPool.numActive)
        assertEquals(10, driverPool.numOnline)

        workingDrivers.forEachIndexed { i, driver ->
            if (i % 2 == 0) driver.retire()
            driverPool.put(driver)
        }

        assertEquals(0, driverPool.numWorking.get())
        assertEquals(5, driverPool.numFree)
        assertEquals(5, driverPool.counterRetired.count)

        driverPool.close()

        assertEquals(0, driverPool.numWorking.get())
        assertEquals(0, driverPool.numFree)
        assertEquals(10, driverPool.counterQuit.count)
    }

    @Ignore("Time consuming (and also bugs)")
    @Test
    fun testWebDriverPoolMultiThreaded() {
        val driverPool = driverPoolManager.createUnmanagedDriverPool()
        val workingDrivers = ArrayBlockingQueue<WebDriver>(30)

        val consumer = Thread {
            while (!quitMultiThreadTesting) {
                if (workingDrivers.size > 20) {
                    TimeUnit.MILLISECONDS.sleep(500)
                }

                if (workingDrivers.size < 20) {
                    driverPool.poll()?.let { workingDrivers.add(it) }
                }
            }
        }

        val producer = Thread {
            while (!quitMultiThreadTesting) {
                val i = Random().nextInt()
                val driver = workingDrivers.poll()
                if (driver != null) {
                    if (i % 3 == 0) {
                        log.info("Offer {}", driver)
                        driverPool.put(driver)
                    } else {
                        log.info("Retire {}", driver)
                        driver.retire()
                        driverPool.put(driver)
                    }
                }
            }
        }

        val closer = Thread {
            while (!quitMultiThreadTesting) {
                log.info("Close all")
                driverPool.close()
                driverPool.close()
                Thread.sleep(1000)
            }
        }

        consumer.isDaemon = true
        producer.isDaemon = true
        closer.isDaemon = true

        consumer.start()
        producer.start()
        closer.start()

        var n = 360
        while (n-- > 0) {
            TimeUnit.SECONDS.sleep(1)
        }

        log.info("All done.")
        quitMultiThreadTesting = true
        producer.join()
        consumer.join()
        closer.join()
    }
}
