package io.github.adeshsuryan.hibernatememorydoctor.report;

import io.github.adeshsuryan.hibernatememorydoctor.ContextEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctor;

/** Human-readable report. Deterministic for identical evidence. */
public final class TextReportRenderer {

    private TextReportRenderer() {
    }

    public static String render(MemoryDoctor.Report report) {
        var ev = report.evidence();
        StringBuilder out = new StringBuilder(512);
        out.append("Hibernate memory doctor\n\n");
        out.append("OPERATION  ").append(ev.label())
                .append(" (").append(ev.scope().name().toLowerCase()).append(" scope)\n");
        out.append("Rows fetched: ").append(format(ev.totalRows()))
                .append("   statements: ").append(ev.totalExecutions())
                .append("   wall time: ").append(ev.wallTimeNanos() / 1_000_000)
                .append(" ms\n");
        if (!ev.contexts().isEmpty()) {
            out.append("Persistence contexts: ").append(ev.contexts().size())
                    .append("   peak managed: ")
                    .append(format(ev.peakManagedEntities()))
                    .append("   entity loads: ")
                    .append(format(ev.probedEntityLoads()))
                    .append("   clears: ")
                    .append(ev.totalClears()).append('\n');
        }
        out.append('\n');
        if (report.findings().isEmpty()) {
            out.append("No findings.\n");
            return out.toString();
        }
        out.append("FINDINGS\n");
        for (Finding f : report.findings()) {
            out.append(pad(f.severity().name(), 6)).append("  ")
                    .append(pad(f.ruleId(), 28)).append("  ")
                    .append(f.evidence()).append('\n');
            out.append("      confidence: ")
                    .append(f.confidence().name().toLowerCase())
                    .append("   ").append(f.why()).append('\n');
        }
        if (!ev.contexts().isEmpty()) {
            out.append("\nCONTEXTS\n");
            for (ContextEvidence c : ev.contexts()) {
                out.append("  ctx ").append(Integer.toHexString(c.contextId()))
                        .append("   peak managed ").append(format(c.peakManagedEntities()))
                        .append("   loads ").append(c.entityLoads())
                        .append("   collections ").append(c.collectionLoads())
                        .append("   clears ").append(c.clears())
                        .append("   span ").append(c.spanNanos() / 1_000_000)
                        .append(" ms\n");
            }
        }
        out.append("\nINVESTIGATE\n");
        int i = 1;
        for (Finding f : report.findings()) {
            for (String step : f.investigate()) {
                out.append(i++).append(". ").append(step).append('\n');
            }
        }
        return out.toString();
    }

    private static String pad(String s, int width) {
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
