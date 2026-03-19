# task-queue

[![CI](https://github.com/philiprehberger/kt-task-queue/actions/workflows/publish.yml/badge.svg)](https://github.com/philiprehberger/kt-task-queue/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.philiprehberger/task-queue)](https://central.sonatype.com/artifact/com.philiprehberger/task-queue)

In-process async task queue with concurrency control and retry.

## Requirements

- Kotlin 1.9+ / Java 17+

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.philiprehberger:task-queue:0.1.0")
}
```

### Maven

```xml
<dependency>
    <groupId>com.philiprehberger</groupId>
    <artifactId>task-queue</artifactId>
    <version>0.1.0</version>
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

## API

| Function / Class | Description |
|------------------|-------------|
| `taskQueue<T> { }` | Build a task queue |
| `TaskQueue.submit(task)` | Submit a task |
| `TaskQueue.stats()` | Get queue statistics |
| `TaskQueue.pause()` / `resume()` | Control processing |
| `TaskQueue.shutdown()` | Shut down the queue |

## Development

```bash
./gradlew test
./gradlew build
```

## License

MIT
