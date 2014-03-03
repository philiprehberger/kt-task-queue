package com.philiprehberger.taskqueue

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Build a task queue with a DSL. */
public fun <T> taskQueue(block: TaskQueueBuilder<T>.() -> Unit): TaskQueue<T> {
    val b = TaskQueueBuilder<T>()
    b.block()
    return TaskQueue(b)
}

public class TaskQueueBuilder<T> {
    internal var concurrency: Int = 4
    internal var handler: (suspend (T) -> Unit)? = null
    internal var maxRetries: Int = 0
    internal var retryDelay: Long = 1000L
    internal var onSuccess: ((T) -> Unit)? = null
    internal var onFailure: ((T, Throwable) -> Unit)? = null

    public fun concurrency(n: Int) { concurrency = n }
    public fun handler(block: suspend (T) -> Unit) { handler = block }
    public fun retry(maxAttempts: Int, delayMs: Long = 1000L) { maxRetries = maxAttempts; retryDelay = delayMs }
    public fun onSuccess(block: (T) -> Unit) { onSuccess = block }
    public fun onFailure(block: (T, Throwable) -> Unit) { onFailure = block }
}

/** In-process async task queue. */
public class TaskQueue<T> internal constructor(private val config: TaskQueueBuilder<T>) {
    private val channel = Channel<T>(Channel.UNLIMITED)
    private val semaphore = Semaphore(config.concurrency)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeCount = AtomicInteger(0)
    private val completedCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)
    @Volatile private var paused = false

    init { startWorkers() }

    private fun startWorkers() {
        scope.launch {
            for (task in channel) {
                while (paused) delay(50)
                semaphore.acquire()
                launch {
                    activeCount.incrementAndGet()
                    try {
                        var lastError: Throwable? = null
                        for (attempt in 0..config.maxRetries) {
                            try { config.handler!!.invoke(task); lastError = null; break }
                            catch (e: Throwable) { lastError = e; if (attempt < config.maxRetries) delay(config.retryDelay) }
                        }
                        if (lastError != null) { failedCount.incrementAndGet(); config.onFailure?.invoke(task, lastError) }
                        else { completedCount.incrementAndGet(); config.onSuccess?.invoke(task) }
                    } finally { activeCount.decrementAndGet(); semaphore.release() }
                }
            }
        }
    }

    /** Submit a task to the queue. */
    public suspend fun submit(task: T) { channel.send(task) }
    /** Get current queue statistics. */
    public fun stats(): TaskStats = TaskStats(0, activeCount.get(), completedCount.get(), failedCount.get())
    /** Pause processing. */
    public fun pause() { paused = true }
    /** Resume processing. */
    public fun resume() { paused = false }
    /** Shut down the queue. */
    public fun shutdown() { channel.close(); scope.cancel() }
}
