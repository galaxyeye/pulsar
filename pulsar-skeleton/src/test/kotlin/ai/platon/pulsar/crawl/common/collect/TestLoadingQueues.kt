package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.collect.ConcurrentNEntrantLoadingQueue
import ai.platon.pulsar.common.collect.FetchCacheCollector
import ai.platon.pulsar.common.collect.LoadingFetchCache
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlAware
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestLoadingQueues : TestBase() {

    @Test
    fun `When create a LoadingFetchCache then the first page is loaded`() {
        val fetchCache = LoadingFetchCache("", urlLoader, group.priority)
        // not loaded
        assertEquals(0, fetchCache.size)
        fetchCache.load()
        assertTrue { fetchCache.size > 0 }
    }

    @Test
    fun `When collect from collector with loading fetch cache then sink has items`() {
        val source = LoadingFetchCache("", urlLoader, group.priority)
        val sink = mutableListOf<UrlAware>()

        assertEquals(0, source.size)
        assertTrue { sink.isEmpty() }

        val collector = FetchCacheCollector(source, source.priority)
        source.loadNow()
        collector.collectTo(sink)

        assertTrue { sink.isNotEmpty() }
    }

    @Test
    fun testConcurrentNEntrantLoadingQueue() {
        val queue = ConcurrentNEntrantLoadingQueue(urlLoader, group)
        queue.load()

        assertTrue { queue.isNotEmpty() }
        var i = 0
        while (i++ < queueSize) {
            val v = queue.poll()
            assertNotNull(v)
            assertTrue { v is Hyperlink }
        }
        assertTrue { queue.isEmpty() }
    }
}
