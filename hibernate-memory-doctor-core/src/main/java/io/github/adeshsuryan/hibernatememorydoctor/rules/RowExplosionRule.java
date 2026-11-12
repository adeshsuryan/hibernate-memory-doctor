package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Confidence;
import io.github.adeshsuryan.hibernatememorydoctor.ContextEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryStatsDelta;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Severity;
import java.util.List;

/**
 * Rows fetched vastly outnumber entity hydrations — the join-multiplication
 * signature (fetching several collections in one query duplicates parent
 * rows combinatorially), or a scalar/native path bypassing entities.
 * Heuristic: row volume is exact but attribution needs a human.
 */
final class RowExplosionRule implements Rule {

    static final String ID = "row-explosion";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        long rows = evidence.totalRows();
        if (rows <= config.totalRowsBudget()) {
            return List.of();
        }
        Long loads = entityLoads(evidence);
        if (loads == null) {
            return List.of();
        }
        boolean scalarPath = loads == 0;
        if (!scalarPath && rows < loads * config.rowExplosionFactor()) {
            return List.of();
        }
        return List.of(new Finding(
                ID,
                Severity.MEDIUM,
                Confidence.HEURISTIC,
                format(rows) + " rows vs "
                        + (scalarPath ? "no entity loads" : format(loads) + " entity loads"),
                scalarPath
                        ? "High row volume with zero entity hydrations — "
                                + "scalar/native reads materialize Object[] rows, "
                                + "which still burn heap without any cache to blame."
                        : "Rows outnumber hydrations ~"
                                + (rows / Math.max(1, loads))
                                + ":1 — typical of fetching multiple collections in one "
                                + "join (cartesian multiplication) or a join on a "
                                + "to-many column.",
                List.of(
                        "Fetching two or more collections in one query "
                                + "explodes rows — split into separate queries or "
                                + "use @BatchSize.",
                        "Check SELECT DISTINCT on to-many joins: it hides the "
                                + "explosion from results but not from the wire.",
                        scalarPath
                                ? "If entities were expected, confirm this path "
                                        + "really goes through the ORM."
                                : "Profile the widest statement — the report "
                                        + "lists per-statement row counts.")));
    }

    private static Long entityLoads(OperationEvidence evidence) {
        MemoryStatsDelta d = evidence.statsDelta();
        if (d != null && d.entityLoads() != null) {
            return d.entityLoads() + (d.entityFetches() != null ? d.entityFetches() : 0);
        }
        int probeLoads = 0;
        boolean any = false;
        for (ContextEvidence c : evidence.contexts()) {
            probeLoads += c.entityLoads();
            any = true;
        }
        return any ? (long) probeLoads : null;
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
