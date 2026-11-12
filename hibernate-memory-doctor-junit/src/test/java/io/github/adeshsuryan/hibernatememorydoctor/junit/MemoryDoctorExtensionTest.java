package io.github.adeshsuryan.hibernatememorydoctor.junit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.adeshsuryan.hibernatememorydoctor.Capture;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctor;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import org.junit.jupiter.api.Test;

class MemoryDoctorExtensionTest {

    private final MemoryDoctor doctor = MemoryDoctor.withConfig(
            MemoryDoctorConfig.builder()
                    .contextGrowthThreshold(50)
                    .entityLoadBudget(50)
                    .build());
    private final MemoryDoctorExtension ext = new MemoryDoctorExtension(doctor);

    /** Simulates the Hibernate probe: three loads, context peaks at 120. */
    private static void growContext() {
        Object session = new Object();
        for (int i = 0; i < 3; i++) {
            Capture.recordEntityLoad(session, 120);
        }
    }

    @Test
    void cleanOperationPassesBudget() {
        assertDoesNotThrow(() ->
                ext.assertBudget("clean", () -> { }, Budget.maxRows(10)
                        .maxManagedEntities(500)
                        .forbidRules("persistence-context-growth")));
    }

    @Test
    void managedEntityViolationFailsWithReport() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                ext.assertBudget("fat op", MemoryDoctorExtensionTest::growContext,
                        Budget.none().maxManagedEntities(100)));
        String message = error.getMessage();
        assertTrue(message.contains("peak managed entities 120 > budget 100"),
                "violation line embedded:\n" + message);
        assertTrue(message.contains("Hibernate memory doctor"),
                "full report embedded in failure:\n" + message);
    }

    @Test
    void entityLoadViolationFails() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                ext.assertBudget("load storm", MemoryDoctorExtensionTest::growContext,
                        Budget.none().maxEntityLoads(2)));
        assertTrue(error.getMessage().contains("entity loads 3 > budget 2"),
                "load budget line:\n" + error.getMessage());
    }

    @Test
    void forbiddenRuleIsCaught() {
        AssertionError error = assertThrows(AssertionError.class, () ->
                ext.assertBudget("ctx growth", MemoryDoctorExtensionTest::growContext,
                        Budget.none().forbidRules("persistence-context-growth")));
        assertTrue(error.getMessage().contains("forbidden rule fired: persistence-context-growth"),
                "rule violation line:\n" + error.getMessage());
    }

    @Test
    void checkReturnsEvaluatedReport() {
        MemoryDoctor.Report report = ext.check("manual", MemoryDoctorExtensionTest::growContext);
        assertTrue(report.findings().stream()
                .anyMatch(f -> f.ruleId().equals("persistence-context-growth")));
        assertTrue(report.evidence().peakManagedEntities() == 120);
    }

    @Test
    void budgetBuilderIsImmutable() {
        Budget b = Budget.maxRows(5).maxManagedEntities(10)
                .maxEntityLoads(20).maxDbTimeMs(100)
                .forbidRules("row-flood", "wide-row-load");
        assertTrue(b.maxRows() == 5 && b.maxManagedEntities() == 10
                && b.maxEntityLoads() == 20 && b.maxDbTimeMs() == 100
                && b.forbidRules().size() == 2);
    }
}
