# Changelog

## [0.1.1] - 2026-03-18

- Upgrade to Kotlin 2.0.21 and Gradle 8.12
- Enable explicitApi() for stricter public API surface
- Add issueManagement to POM metadata

## [0.1.0] - 2026-03-18

### Added

- `taskQueue {}` DSL for building task queues

- Configurable concurrency limit

- Retry with delay on failure

- Lifecycle callbacks: onSuccess, onFailure

- `stats()` for queue metrics

- `pause()` / `resume()` / `shutdown()` control
