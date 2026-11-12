# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - Unreleased

### Added

- Three modules: `hibernate-memory-doctor-core` (zero required
  dependencies), `hibernate-memory-doctor-spring-boot-starter`
  (Spring Boot 3.x auto-configuration), and `hibernate-memory-doctor-junit`
  (budget assertions).
- Operation boundaries (`request`, `manual`, `test` scopes) with
  thread-local capture and immutable evidence records.
- JDBC capture via `DoctorDataSource` JDK proxies: statement executions,
  per-statement row materialization counts, SQL normalization that
  structurally removes bind values and literals.
- Persistence-context probe (Spring Boot starter) registered on the
  Hibernate `EventListenerRegistry`: `POST_LOAD` hydration sampling of
  `getNumberOfManagedEntities()`, `INIT_COLLECTION` collection-size
  capture, `CLEAR`/`EVICT` discipline tracking, per-context peak managed
  entities.
- Hibernate `Statistics` delta bridge for entity loads/fetches,
  collection loads/fetches, flushes, and query executions.
- Nine memory rules: `persistence-context-growth`, `entity-load-storm`,
  `collection-load-heavy`, `oversized-collection`, `row-flood`,
  `wide-row-load`, `row-explosion`, `bulk-load-without-clear`,
  `long-lived-context` — each carrying `confidence: exact | heuristic`
  plus investigate/fix guidance.
- Text and stable-schema JSON report renderers.
- `MemoryDoctorExtension` + `Budget` (max rows, max managed entities,
  max entity loads, max DB time, forbidden rule IDs); assertion failures
  embed the full report.
- Configuration properties under `hibernate-memory-doctor.*` including
  `enabled`, `probe`, and per-rule thresholds.
