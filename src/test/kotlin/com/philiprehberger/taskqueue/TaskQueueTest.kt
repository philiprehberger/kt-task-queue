package com.philiprehberger.taskqueue

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlin.test.*

class TaskQueueTest {
    @Test fun `basic submit and process`() = runTest {
        val processed = mutableListOf<Int>()
        val q = taskQueue<Int> {
            concurrency(2)
            handler { processed.add(it) }
        }
        q.submit(1); q.submit(2); q.submit(3)
        delay(500)
        q.shutdown()
        assertEquals(3, processed.size)
    }
}
