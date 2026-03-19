package com.philiprehberger.taskqueue

/** Snapshot of queue statistics. */
public data class TaskStats(
    public val pending: Int,
    public val active: Int,
    public val completed: Long,
    public val failed: Long,
)
