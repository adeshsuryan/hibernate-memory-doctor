package io.github.adeshsuryan.hibernatememorydoctor;

import io.github.adeshsuryan.hibernatememorydoctor.jdbc.DoctorDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof of the zero-dependency JDBC capture: real H2 database,
 * real PreparedStatements, no framework.
 */
class JdbcCaptureTest {

    private DataSource raw;
    private DoctorDataSource ds;

    @BeforeEach
    void setup() throws Exception {
        JdbcDataSource h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:mdcap" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        h2.setUser("sa");
        raw = h2;
        ds = DoctorDataSource.wrap(raw);
        try (Connection c = raw.getConnection();
             var st = c.createStatement()) {
            st.execute("CREATE TABLE item (id INT PRIMARY KEY, name VARCHAR(64))");
            for (int i = 1; i <= 2_000; i++) {
                st.execute("INSERT INTO item VALUES (" + i + ", 'i" + i + "')");
            }
        }
    }

    @Test
    void countsRowsFetchedPerStatement() {
        MemoryDoctor.Report report = MemoryDoctor.get().check("wide fetch", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM item");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rs.getString(2);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(2_000, report.evidence().totalRows());
        assertEquals(1, report.evidence().statements().size());
        assertTrue(report.json().contains("\"totalRowsFetched\": 2000"));
    }

    @Test
    void rowFloodFiresOverBudget() {
        MemoryDoctor doctor = MemoryDoctor.withConfig(
                MemoryDoctorConfig.builder().totalRowsBudget(100).build());
        MemoryDoctor.Report report = doctor.check("row flood repro", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM item");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rs.getInt(1);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.ruleId().equals("row-flood")));
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.ruleId().equals("wide-row-load")));
        String text = report.text();
        assertTrue(text.contains("Hibernate memory doctor"));
        assertTrue(text.contains("Rows fetched: 2,000"));
    }

    @Test
    void nothingCapturedOutsideBoundary() throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM item");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
        }
        MemoryDoctor.Report report = MemoryDoctor.get().check("empty", () -> { });
        assertEquals(0, report.evidence().totalRows());
        assertEquals(0, report.evidence().totalExecutions());
        assertFalse(report.hasFindings());
    }

    @Test
    void contextProbeEventsRecordedInsideBoundary() {
        MemoryDoctor doctor = MemoryDoctor.withConfig(
                MemoryDoctorConfig.builder().contextGrowthThreshold(10).build());
        Object fakeSession = new Object();
        MemoryDoctor.Report report = doctor.check("probe repro", () -> {
            for (int i = 0; i < 50; i++) {
                Capture.recordEntityLoad(fakeSession, i + 1);
            }
            Capture.recordCollectionLoad(fakeSession, 2_500);
            Capture.recordClear(fakeSession);
        });
        assertEquals(1, report.evidence().contexts().size());
        ContextEvidence ctx = report.evidence().contexts().get(0);
        assertEquals(50, ctx.peakManagedEntities());
        assertEquals(2_500, ctx.peakCollectionSize());
        assertEquals(1, ctx.clears());
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.ruleId().equals("persistence-context-growth")));
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.ruleId().equals("oversized-collection")));
    }

    @Test
    void bindValuesNeverCaptured() {
        MemoryDoctor.Report report = MemoryDoctor.get().check("privacy", () -> {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM item WHERE name = ?")) {
                ps.setString(1, "secret-value-123");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rs.getInt(1);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        String json = report.json();
        assertFalse(json.contains("secret-value-123"));
        assertTrue(report.evidence().statements().get(0).normalizedSql()
                .contains("name = ?"));
    }
}
