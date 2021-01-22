package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.url.UrlAware
import com.google.common.collect.HashMultiset

class ConcurrentLoadingQueue(
        loader: ExternalUrlLoader,
        group: Int = ConcurrentLoadingQueue::javaClass.name.hashCode(),
        priority: Int = Priority13.NORMAL.value,
        capacity: Int = LoadingQueue.DEFAULT_CAPACITY
): AbstractLoadingQueue(loader, group, priority, capacity)

class ConcurrentNonReentrantLoadingQueue(
        loader: ExternalUrlLoader,
        group: Int = ConcurrentNonReentrantLoadingQueue::javaClass.name.hashCode(),
        priority: Int = Priority13.NORMAL.value,
        capacity: Int = LoadingQueue.DEFAULT_CAPACITY
): AbstractLoadingQueue(loader, group, priority, capacity) {
    private val historyHash = HashSet<Int>()

    @Synchronized
    fun count(url: UrlAware) = if (historyHash.contains(url.hashCode())) 1 else 0

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (!historyHash.contains(hashCode)) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.add(hashCode)
                cache.add(url)
            } else {
                loader.save(url, group)
                true
            }
        }

        return false
    }
}

class ConcurrentNEntrantLoadingQueue(
        loader: ExternalUrlLoader,
        val n: Int = 3,
        group: Int = ConcurrentNEntrantLoadingQueue::javaClass.name.hashCode(),
        priority: Int = Priority13.NORMAL.value,
        capacity: Int = LoadingQueue.DEFAULT_CAPACITY
): AbstractLoadingQueue(loader, group, priority, capacity) {

    private val historyHash = HashMultiset.create<Int>()

    @Synchronized
    fun count(url: UrlAware) = historyHash.count(url.hashCode())

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (historyHash.count(hashCode) <= n) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.add(hashCode)
                cache.add(url)
            } else {
                loader.save(url, group)
                true
            }
        }

        return false
    }
}
