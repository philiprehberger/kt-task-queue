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
    internal var onDeadLetter: ((T, Throwable) -> Unit)? = null

    public fun concurrency(n: Int) { concurrency = n }
    public fun handler(block: suspend (T) -> Unit) { handler = block }
    public fun retry(maxAttempts: Int, delayMs: Long = 1000L) { maxRetries = maxAttempts; retryDelay = delayMs }
    public fun onSuccess(block: (T) -> Unit) { onSuccess = block }
    public fun onFailure(block: (T, Throwable) -> Unit) { onFailure = block }

    /**
     * Sets a handler for tasks that exhaust all retry attempts.
     *
     * @param block Callback invoked with the task and the last error.
     */
    public fun onDeadLetter(block: (T, Throwable) -> Unit) { onDeadLetter = block }
}

/** In-process async task queue with concurrency control, retry, and dead letter handling. */
public class TaskQueue<T> internal constructor(private val config: TaskQueueBuilder<T>) {
    private val channel = Channel<T>(Channel.UNLIMITED)
    private val semaphore = Semaphore(config.concurrency)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeCount = AtomicInteger(0)
    private val completedCount = AtomicLong(0)
    private val failedCount = AtomicLong(0)
    private val deadLetterCount = AtomicLong(0)
    @Volatile private var paused = false
    private val activeJobs = java.util.concurrent.ConcurrentLinkedQueue<Job>()

    init { startWorkers() }

    private fun startWorkers() {
        scope.launch {
            for (task in channel) {
                while (paused) delay(50)
                semaphore.acquire()
                val job = launch {
                    activeCount.incrementAndGet()
                    try {
                        var lastError: Throwable? = null
                        for (attempt in 0..config.maxRetries) {
                            try { config.handler!!.invoke(task); lastError = null; break }
                            catch (e: Throwable) { lastError = e; if (attempt < config.maxRetries) delay(config.retryDelay) }
                        }
                        if (lastError != null) {
                            failedCount.incrementAndGet()
                            config.onFailure?.invoke(task, lastError)
                            if (config.onDeadLetter != null) {
                                deadLetterCount.incrementAndGet()
                                config.onDeadLetter!!.invoke(task, lastError)
                            }
                        } else {
                            completedCount.incrementAndGet()
                            config.onSuccess?.invoke(task)
                        }
                    } finally {
                        activeCount.decrementAndGet()
                        semaphore.release()
                    }
                }
                activeJobs.add(job)
                job.invokeOnCompletion { activeJobs.remove(job) }
            }
        }
    }

    /** Submit a task to the queue. */
    public suspend fun submit(task: T) { channel.send(task) }

    /**
     * Submit multiple tasks to the queue.
     *
     * @param tasks The collection of tasks to submit.
     */
    public suspend fun submitAll(tasks: Collection<T>) {
        for (task in tasks) {
            channel.send(task)
        }
    }

    /**
     * Suspends until all currently submitted tasks have been processed.
     *
     * New tasks submitted after calling [drain] are not waited for.
     */
    public suspend fun drain() {
        // Wait for channel to be empty and all active jobs to complete
        while (activeCount.get() > 0 || !channel.isEmpty) {
            delay(10)
        }
        // Wait for any remaining active jobs
        activeJobs.toList().forEach { it.join() }
    }

    /** Get current queue statistics. */
    public fun stats(): TaskStats = TaskStats(0, activeCount.get(), completedCount.get(), failedCount.get())
    /** Pause processing. */
    public fun pause() { paused = true }
    /** Resume processing. */
    public fun resume() { paused = false }
    /** Shut down the queue. */
    public fun shutdown() { channel.close(); scope.cancel() }
}
