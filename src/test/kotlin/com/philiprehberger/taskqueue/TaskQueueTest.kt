package com.philiprehberger.taskqueue

import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
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
}
