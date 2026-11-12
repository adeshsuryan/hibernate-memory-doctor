package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Confidence;
import io.github.adeshsuryan.hibernatememorydoctor.ContextEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Severity;
import java.util.ArrayList;
import java.util.List;

/**
 * A persistence context accumulated more managed entities than the
 * threshold. Every managed entity is a live object reachable from the
 * Session — the direct heap-growth path in batch loops and unbounded
 * loads. Exact: the count was sampled at hydration events.
 */
final class PersistenceContextGrowthRule implements Rule {

    static final String ID = "persistence-context-growth";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (ContextEvidence c : evidence.contexts()) {
            if (c.peakManagedEntities() <= config.contextGrowthThreshold()) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.HIGH,
                    Confidence.EXACT,
                    "context held up to " + format(c.peakManagedEntities())
                            + " managed entities",
                    "Every managed entity is reachable from the persistence "
                            + "context and cannot be garbage-collected until the "
                            + "context closes or is cleared — this is the direct "
                            + "heap-growth path.",
                    List.of(
                            "Batch loop? Call entityManager.clear() (or "
                                    + "Session.evict) every N items — typically every "
                                    + "jdbc.batch_size rows.",
                            "Read-only bulk work? Consider StatelessSession — it "
                                    + "keeps no first-level cache at all.",
                            "If the context is per-request, check for entities "
                                    + "loaded incidentally via lazy associations.")));
        }
        return out;
    }

    private static String format(int n) {
        return String.format("%,d", n);
    }
}
