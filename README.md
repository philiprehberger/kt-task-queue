# task-queue

[![Tests](https://github.com/philiprehberger/kt-task-queue/actions/workflows/publish.yml/badge.svg)](https://github.com/philiprehberger/kt-task-queue/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.philiprehberger/task-queue.svg)](https://central.sonatype.com/artifact/com.philiprehberger/task-queue)
[![Last updated](https://img.shields.io/github/last-commit/philiprehberger/kt-task-queue)](https://github.com/philiprehberger/kt-task-queue/commits/main)

In-process async task queue with concurrency control and retry.

## Installation

### Gradle (Kotlin DSL)

```kotlin
implementation("com.philiprehberger:task-queue:0.2.0")
```

### Maven

```xml
<dependency>
    <groupId>com.philiprehberger</groupId>
    <artifactId>task-queue</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Usage

```kotlin
import com.philiprehberger.taskqueue.*

val queue = taskQueue<String> {
    concurrency(4)
    handler { task -> processTask(task) }
    retry(maxAttempts = 3)
    onSuccess { println("Done: $it") }
    onFailure { task, err -> println("Failed: $task") }
}

queue.submit("task-1")
queue.stats() // pending, active, completed, failed
```

### Batch Submit

```kotlin
queue.submitAll(listOf("task-1", "task-2", "task-3"))
```

### Drain

```kotlin
queue.submit("task-1")
queue.submit("task-2")
queue.drain() // Suspends until all tasks are processed
```

### Dead Letter Handling

```kotlin
val queue = taskQueue<String> {
    concurrency(4)
    handler { processTask(it) }
    retry(maxAttempts = 3, delayMs = 1000)
    onDeadLetter { task, error ->
        log.error("Task permanently failed: $task", error)
    }
}
```

## API

| Function / Class | Description |
|------------------|-------------|
| `taskQueue<T> { }` | Build a task queue |
| `TaskQueue.submit(task)` | Submit a task |
| `TaskQueue.stats()` | Get queue statistics |
| `TaskQueue.pause()` / `resume()` | Control processing |
| `TaskQueue.submitAll(tasks)` | Submit multiple tasks at once |
| `TaskQueue.drain()` | Wait for all tasks to complete |
| `TaskQueue.shutdown()` | Shut down the queue |
| `onDeadLetter { }` | Handle tasks that exhaust retries |

## Development

```bash
./gradlew test
./gradlew build
```

## Support

If you find this project useful:

⭐ [Star the repo](https://github.com/philiprehberger/kt-task-queue)

🐛 [Report issues](https://github.com/philiprehberger/kt-task-queue/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

💡 [Suggest features](https://github.com/philiprehberger/kt-task-queue/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement)

❤️ [Sponsor development](https://github.com/sponsors/philiprehberger)

🌐 [All Open Source Projects](https://philiprehberger.com/open-source-packages)

💻 [GitHub Profile](https://github.com/philiprehberger)

🔗 [LinkedIn Profile](https://www.linkedin.com/in/philiprehberger)

## License

[MIT](LICENSE)
