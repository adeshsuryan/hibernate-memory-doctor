package io.github.adeshsuryan.hibernatememorydoctor;

import io.github.adeshsuryan.hibernatememorydoctor.report.JsonReportRenderer;
import io.github.adeshsuryan.hibernatememorydoctor.report.TextReportRenderer;
import io.github.adeshsuryan.hibernatememorydoctor.rules.RuleRegistry;
import java.util.List;
import java.util.function.Supplier;

/**
 * Entry point. Configure once, then open boundaries around the operations
 * you want diagnosed. A lazily-created shared instance backs the static
 * helpers; Spring users get a configured bean instead.
 */
public final class MemoryDoctor {

    private static volatile MemoryDoctor shared;

    private final MemoryDoctorConfig config;
    private final RuleRegistry rules;
    private final Supplier<MemoryStatsDelta> statsSource;

    private MemoryDoctor(MemoryDoctorConfig config, Supplier<MemoryStatsDelta> statsSource) {
        this.config = config;
        this.rules = new RuleRegistry();
        this.statsSource = statsSource;
    }

    public static MemoryDoctor get() {
        MemoryDoctor d = shared;
        if (d == null) {
            synchronized (MemoryDoctor.class) {
                d = shared;
                if (d == null) {
                    d = new MemoryDoctor(MemoryDoctorConfig.defaults(), null);
                    shared = d;
                }
            }
        }
        return d;
    }

    public static MemoryDoctor withConfig(MemoryDoctorConfig config) {
        return new MemoryDoctor(config, null);
    }

    public static MemoryDoctor withConfig(MemoryDoctorConfig config,
                                          Supplier<MemoryStatsDelta> statsSource) {
        return new MemoryDoctor(config, statsSource);
    }

    /** Only for tests and the Spring starter. */
    public static void setShared(MemoryDoctor doctor) {
        shared = doctor;
    }

    public MemoryDoctorConfig config() {
        return config;
    }

    /** Open a manual boundary. Close via try-with-resources. */
    public OperationBoundary open(String label) {
        return open(label, Scope.MANUAL, false);
    }

    public OperationBoundary open(String label, Scope scope, boolean readOnly) {
        return new OperationBoundary(scope, label, readOnly, statsSource);
    }

    /** Run an operation inside a TEST boundary; returns evaluated report. */
    public Report check(String label, Runnable operation) {
        OperationEvidence evidence;
        try (OperationBoundary b = open(label, Scope.TEST, false)) {
            operation.run();
            evidence = b.evidence();
        }
        return report(evidence);
    }

    public Report check(String label, Scope scope, boolean readOnly, Runnable operation) {
        OperationEvidence evidence;
        try (OperationBoundary b = open(label, scope, readOnly)) {
            operation.run();
            evidence = b.evidence();
        }
        return report(evidence);
    }

    /** Evaluate rules against captured evidence. */
    public Report report(OperationEvidence evidence) {
        return new Report(evidence, rules.evaluate(evidence, config));
    }

    /** An evaluated report: evidence plus findings plus renderers. */
    public record Report(OperationEvidence evidence, List<Finding> findings) {

        public Report {
            findings = findings == null ? List.of() : List.copyOf(findings);
        }

        public boolean hasFindings() {
            return !findings.isEmpty();
        }

        public String text() {
            return TextReportRenderer.render(this);
        }

        public String json() {
            return JsonReportRenderer.render(this);
        }
    }
}
