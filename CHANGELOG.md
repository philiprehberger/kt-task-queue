# Changelog

## 0.1.4 (2026-03-22)

- Fix README compliance (badge label, installation format)

## 0.1.3 (2026-03-20)

- Fix test: use runBlocking and thread-safe collection instead of runTest with virtual time

## 0.1.2 (2026-03-20)

- Standardize README: fix title, badges, version sync, remove Requirements section

## 0.1.1 (2026-03-18)

- Upgrade to Kotlin 2.0.21 and Gradle 8.12
- Enable explicitApi() for stricter public API surface
- Add issueManagement to POM metadata

## 0.1.0 (2026-03-18)

- `taskQueue {}` DSL for building task queues
- Configurable concurrency limit
- Retry with delay on failure
- Lifecycle callbacks: onSuccess, onFailure
- `stats()` for queue metrics
- `pause()` / `resume()` / `shutdown()` control
