package io.github.adeshsuryan.hibernatememorydoctor;

import io.github.adeshsuryan.hibernatememorydoctor.jdbc.DoctorDataSource;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack proof: real Spring Boot app, real Hibernate ORM, real H2.
 * Batch-style loads grow the persistence context past test thresholds and
 * the probe reports the true peak - this is also the task-04 spike: it
 * validates POST_LOAD coverage on query results, getSize() on
 * POST_COLLECTION_LOAD, CLEAR on the JPA path, and context identity.
 */
@SpringBootTest(classes = TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mditdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "hibernate-memory-doctor.report-mode=off",
                "hibernate-memory-doctor.context-growth-threshold=50",
                "hibernate-memory-doctor.bulk-load-threshold=50",
                "hibernate-memory-doctor.entity-load-budget=50",
                "hibernate-memory-doctor.wide-row-threshold=100",
                "hibernate-memory-doctor.total-rows-budget=1000",
                "hibernate-memory-doctor.collection-size-threshold=50"
        })
class HibernateMemoryDoctorIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MemoryDoctor doctor;

    @Autowired
    private ImportService importService;

    @Autowired
    private ItemRepository items;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private LineRepository lines;

    @BeforeEach
    @Transactional
    void seed() {
        if (items.count() == 0) {
            IntStream.rangeClosed(1, 200)
                    .forEach(i -> items.save(new Item(i, "seed-" + i)));
        }
        if (orders.count() == 0) {
            DocOrder order = orders.save(new DocOrder(1));
            IntStream.rangeClosed(1, 120)
                    .forEach(i -> lines.save(new OrderLine(i, order)));
        }
    }

    @Test
    void dataSourceIsWrappedByAutoConfiguration() {
        assertInstanceOf(DoctorDataSource.class, dataSource);
    }

    @Test
    void bulkHydrationGrowsContextAndFiresMemoryRules() {
        MemoryDoctor.Report report = doctor.check("bulk load all",
                importService::bulkLoadAll);
        assertTrue(report.hasFindings(), "expected findings, got:\n" + report.text());
        assertTrue(report.findings().stream()
                        .anyMatch(f -> f.ruleId().equals("persistence-context-growth")),
                "expected persistence-context-growth in:\n" + report.text());
        assertTrue(report.findings().stream()
                        .anyMatch(f -> f.ruleId().equals("bulk-load-without-clear")),
                "expected bulk-load-without-clear in:\n" + report.text());
        // POST_LOAD fired per hydrated entity - probe and stats both saw loads
        assertTrue(report.evidence().peakManagedEntities() > 50,
                "probe measured peak, got: " + report.evidence().peakManagedEntities());
        assertEquals(1, report.evidence().contexts().size(),
                "one persistence context across the service call");
        System.out.println(report.text());
    }

    @Test
    void fatCollectionIsNamedBySize() {
        MemoryDoctor.Report report = doctor.check("fat collection", () -> {
            // touching the lazy collection materializes all 120 lines
            importService.touchLines(1);
        });
        assertTrue(report.findings().stream()
                        .anyMatch(f -> f.ruleId().equals("oversized-collection")),
                "expected oversized-collection in:\n" + report.text());
        assertTrue(report.evidence().contexts().stream()
                        .anyMatch(c -> c.peakCollectionSize() >= 120),
                "collection size measured at materialization");
    }

    @Test
    void clearCallsSuppressBulkLoadFinding() {
        MemoryDoctor.Report report = doctor.check("disciplined batch", () ->
                importService.disciplinedPersist(10_000, 100));
        assertTrue(report.evidence().totalClears() > 0,
                "em.clear() should produce CLEAR events");
        assertTrue(report.findings().stream()
                        .noneMatch(f -> f.ruleId().equals("bulk-load-without-clear")),
                "discipline present -> bulk-load-without-clear must stay silent:\n"
                        + report.text());
    }

    @Test
    void cleanPathStaysSilent() {
        MemoryDoctor.Report report = doctor.check("item count", importService::count);
        assertFalse(report.hasFindings(), "expected no findings, got:\n" + report.text());
    }
}
