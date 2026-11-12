package io.github.adeshsuryan.hibernatememorydoctor.junit;

import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for memory-budget assertions.
 *
 * <pre>{@code
 * @RegisterExtension
 * MemoryDoctorExtension doctor = new MemoryDoctorExtension();
 *
 * @Test
 * void exportStaysInBudget() {
 *     doctor.assertBudget("customer export",
 *             () -> exportService.run(),
 *             Budget.maxRows(500).forbidRules("row-flood"));
 * }
 * }</pre>
 *
 * On violation the assertion error embeds the full text report — the
 * failure IS the diagnosis.
 */
public final class MemoryDoctorExtension implements AfterEachCallback {

    private final MemoryDoctor doctor;

    public MemoryDoctorExtension() {
        this(MemoryDoctor.get());
    }

    public MemoryDoctorExtension(MemoryDoctor doctor) {
        this.doctor = doctor;
    }

    /** Run an operation inside a TEST boundary and evaluate rules. */
    public MemoryDoctor.Report check(String label, Runnable operation) {
        return doctor.check(label, operation);
    }

    /** Assert the operation stays inside the budget; fails with the report. */
    public void assertBudget(String label, Runnable operation, Budget budget) {
        MemoryDoctor.Report report = check(label, operation);
        List<String> violations = violations(report, budget);
        if (!violations.isEmpty()) {
            throw new AssertionError(
                    "hibernate-memory-doctor budget violation in '" + label + "':\n"
                            + String.join("\n", violations)
                            + "\n\n" + report.text());
        }
    }

    private List<String> violations(MemoryDoctor.Report report, Budget budget) {
        List<String> out = new ArrayList<>();
        if (budget.maxRows() != null
                && report.evidence().totalRows() > budget.maxRows()) {
            out.add("  rows fetched " + report.evidence().totalRows()
                    + " > budget " + budget.maxRows());
        }
        if (budget.maxManagedEntities() != null
                && report.evidence().peakManagedEntities() > budget.maxManagedEntities()) {
            out.add("  peak managed entities " + report.evidence().peakManagedEntities()
                    + " > budget " + budget.maxManagedEntities());
        }
        if (budget.maxEntityLoads() != null) {
            long loads = Math.max(
                    report.evidence().probedEntityLoads(),
                    report.evidence().statsDelta() == null
                            || report.evidence().statsDelta().entityLoads() == null
                            ? 0L : report.evidence().statsDelta().entityLoads());
            if (loads > budget.maxEntityLoads()) {
                out.add("  entity loads " + loads + " > budget " + budget.maxEntityLoads());
            }
        }
        if (budget.maxDbTimeMs() != null
                && report.evidence().dbTimeNanos() > budget.maxDbTimeMs() * 1_000_000L) {
            out.add("  db time " + (report.evidence().dbTimeNanos() / 1_000_000)
                    + " ms > budget " + budget.maxDbTimeMs() + " ms");
        }
        for (Finding f : report.findings()) {
            if (budget.forbidRules().contains(f.ruleId())) {
                out.add("  forbidden rule fired: " + f.ruleId()
                        + " (" + f.evidence() + ")");
            }
        }
        return out;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        // Boundaries auto-close in check(); nothing to clean up here.
        // Hook kept for future per-test reporting modes.
    }
}
