# hibernate-memory-doctor

<p align="center">
  <img src="docs/assets/banner.svg" alt="hibernate-memory-doctor" width="900">
</p>

Your Spring Boot + Hibernate app hums along — then a batch job or an
unbounded export hydrates 50,000 entities into one persistence context,
and the JVM dies of `OutOfMemoryError`. Hibernate's own documentation
opens its batch chapter with exactly this failure. Nothing warned you:
every `persist()` and `load()` was individually correct.

**hibernate-memory-doctor** measures first-level-cache residency *per
operation* — peak managed entities, rows materialized, entity/collection
hydration, and `clear()`/`evict()` discipline — and reports findings
with evidence and fix guidance, *before* the heap dump. Sibling of
[hibernate-doctor](https://github.com/adeshsuryan/hibernate-doctor)
(SQL/query shape) and
[spring-db-pool-doctor](https://github.com/adeshsuryan/spring-db-pool-doctor)
(connection pool).

```text
Hibernate memory doctor

OPERATION  bulk load all (test scope)
Rows fetched: 300   statements: 1   wall time: 32 ms

FINDINGS
HIGH    persistence-context-growth    context held up to 300 managed entities
        confidence: exact   Every managed entity is reachable from the
                            persistence context and cannot be garbage-collected
                            until the context closes or is cleared.
MEDIUM  bulk-load-without-clear       300 entity loads, 0 clear/evict calls
MEDIUM  entity-load-storm             300 entity hydrations in one operation
MEDIUM  wide-row-load                 select i1_0.id,i1_0.name from item i1_0 -> 300 rows

CONTEXTS
  ctx 15cba6dc   peak managed 300   loads 300   collections 0   clears 0   span 0 ms

INVESTIGATE
1. Batch loop? Call entityManager.clear() every N items — typically every jdbc.batch_size rows.
2. Read-only bulk work? Consider StatelessSession — it keeps no first-level cache at all.
3. DTO projections skip hydration entirely for read-only paths.
```

## Install

Maven:

```xml
<dependency>
    <groupId>io.github.adeshsuryan</groupId>
    <artifactId>hibernate-memory-doctor-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Gradle:

```kotlin
implementation("io.github.adeshsuryan:hibernate-memory-doctor-spring-boot-starter:0.1.0")
```

That's it on Spring Boot 3.x — the starter wraps your `DataSource`,
registers a Hibernate event probe (`POST_LOAD`, `INIT_COLLECTION`,
`CLEAR`, `EVICT`) that samples `getNumberOfManagedEntities()` at
hydration time, scopes every HTTP request, and logs findings
automatically.

## What it detects (v1 rules)

| Rule | Signal | Evidence |
| ---- | ------ | -------- |
| `persistence-context-growth` | Peak managed entities over threshold | exact — sampled at hydration events |
| `entity-load-storm` | Many hydrations in one operation | exact (probe) / stats delta |
| `collection-load-heavy` | Many collection initializations | stats delta |
| `oversized-collection` | One collection materialized beyond threshold | exact — `INIT_COLLECTION` size |
| `row-flood` | Total rows materialized over budget | exact — JDBC row counting |
| `wide-row-load` | One statement materialized a flood | exact — per-statement rows |
| `row-explosion` | Rows ≫ entity loads — join multiplication | heuristic |
| `bulk-load-without-clear` | Bulk loads with zero `clear()`/`evict()` | heuristic |
| `long-lived-context` | One context spanned beyond threshold | heuristic |

Every finding carries `confidence: exact | heuristic` — exact findings
are measured; heuristic ones are named as patterns, not proven root
causes.

## Configuration

```properties
hibernate-memory-doctor.enabled=true                    # master switch
hibernate-memory-doctor.probe=true                      # Hibernate event probe
hibernate-memory-doctor.context-growth-threshold=5000   # peak managed entities
hibernate-memory-doctor.bulk-load-threshold=5000        # loads w/o clear heuristic
hibernate-memory-doctor.total-rows-budget=10000         # row-flood
hibernate-memory-doctor.wide-row-threshold=1000         # wide-row-load
hibernate-memory-doctor.entity-load-budget=2000         # entity-load-storm
hibernate-memory-doctor.collection-load-budget=500      # collection-load-heavy
hibernate-memory-doctor.collection-size-threshold=2000  # oversized-collection
hibernate-memory-doctor.row-explosion-factor=10         # rows per entity
hibernate-memory-doctor.context-span-ms=5000            # long-lived-context
hibernate-memory-doctor.report-mode=log-on-findings     # log-on-findings | log-always | off
```

## Pin a memory budget in a test

```java
@RegisterExtension
MemoryDoctorExtension doctor = new MemoryDoctorExtension();

@Test
void exportStaysInBudget() {
    doctor.assertBudget("customer export",
            () -> exportService.run(),
            Budget.maxRows(500)
                  .maxManagedEntities(200)
                  .forbidRules("row-flood", "persistence-context-growth"));
}
```

On violation the assertion error **is** the diagnosis — it embeds the
full report above.

## Without Spring Boot

```java
DataSource ds = DoctorDataSource.wrap(realDataSource);

MemoryDoctor.Report report = MemoryDoctor.get().check("order lookup", () -> {
    // any JDBC / Hibernate / JPA work through ds
});
System.out.println(report.text());   // or report.json() — stable schema
```

## Boundary vs. hibernate-doctor

These are siblings, not duplicates. `hibernate-doctor` answers "which
SQL shape hurts" (N+1, slow queries, repeated statements). This package
answers "what did the persistence context retain" (managed-entity
residency, hydration volume, clear discipline). Run both — they share no
evidence and their findings don't overlap.

## Privacy & safety

- **Bind values are never captured.** The capture layer only ever sees
  `?` placeholders; raw literals are normalized out before evidence is
  built. There is no flag to enable it — it's structurally absent.
- **Read-only.** It never mutates queries, entities, or the persistence
  context. `CLEAR`/`EVICT` events are observed, never triggered.
- **Zero required dependencies** in `hibernate-memory-doctor-core`.
- Disabled = no wrapping, no probe, near-zero overhead.

## Modules

| Artifact | Purpose |
| -------- | ------- |
| `hibernate-memory-doctor-core` | Evidence model, context traces, JDBC capture, rules, renderers |
| `hibernate-memory-doctor-spring-boot-starter` | Boot 3.x auto-config: DataSource wrap, request filter, Hibernate event probe, properties |
| `hibernate-memory-doctor-junit` | `MemoryDoctorExtension` + `Budget` assertions |

## Compatibility

| Tested | |
| ------ | --- |
| Java | 17, 21 (CI matrix) |
| Spring Boot | 3.2.x – 3.5.x (CI matrix) |
| Hibernate ORM | 6.x (Boot-managed; probe uses `POST_LOAD`/`INIT_COLLECTION`/`CLEAR`/`EVICT`) |
| Database | Any JDBC — proof suite runs H2 |

## Roadmap

- **0.2** — entity/association attribution (`suspect: Order.lines`)
- **0.3** — heap-pressure correlation (context residency vs. old-gen growth)
- **0.4** — CI gate semantics, documented Actions usage
- **0.5** — Boot 4.x / Hibernate 7.x matrix, MySQL/PostgreSQL fixtures
- **1.0** — stable rule IDs + JSON schema across two Boot minors

## License

[Apache-2.0](LICENSE)
