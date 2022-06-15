package com.philiprehberger.taskqueue

import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class TaskQueueTest {
    @Test fun `basic submit and process`() = runBlocking {
        val processed = CopyOnWriteArrayList<Int>()
        val q = taskQueue<Int> {
            concurrency(2)
            handler { processed.add(it) }
        }
        q.submit(1); q.submit(2); q.submit(3)
        Thread.sleep(500)
        q.shutdown()
        assertEquals(3, processed.size)
    }

    @Test fun `submitAll submits multiple tasks`() = runBlocking {
        val processed = CopyOnWriteArrayList<Int>()
        val q = taskQueue<Int> {
            concurrency(2)
            handler { processed.add(it) }
        }
        q.submitAll(listOf(1, 2, 3, 4, 5))
        Thread.sleep(500)
        q.shutdown()
        assertEquals(5, processed.size)
    }

    @Test fun `drain waits for all tasks`() = runBlocking {
        val processed = CopyOnWriteArrayList<Int>()
        val q = taskQueue<Int> {
            concurrency(2)
            handler {
                Thread.sleep(50)
                processed.add(it)
            }
        }
        q.submit(1); q.submit(2); q.submit(3)
        q.drain()
        assertEquals(3, processed.size)
        q.shutdown()
    }

    @Test fun `onDeadLetter receives failed tasks`() = runBlocking {
        val deadLetters = CopyOnWriteArrayList<String>()
        val q = taskQueue<String> {
            concurrency(1)
            handler { throw RuntimeException("fail") }
            retry(maxAttempts = 1, delayMs = 10)
            onDeadLetter { task, _ -> deadLetters.add(task) }
        }
        q.submit("task-1")
        Thread.sleep(500)
        q.shutdown()
        assertEquals(1, deadLetters.size)
        assertEquals("task-1", deadLetters[0])
    }

    @Test fun `stats tracks completed and failed`() = runBlocking {
        val q = taskQueue<Int> {
            concurrency(2)
            handler { if (it == 2) throw RuntimeException("fail") }
        }
        q.submit(1); q.submit(2); q.submit(3)
        Thread.sleep(500)
        val stats = q.stats()
        assertEquals(2L, stats.completed)
        assertEquals(1L, stats.failed)
        q.shutdown()
    }
}
