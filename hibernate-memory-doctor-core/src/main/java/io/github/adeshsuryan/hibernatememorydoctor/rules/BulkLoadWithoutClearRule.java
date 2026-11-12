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
 * Many entities loaded in one operation with no observed clear/evict —
 * the batch-import signature. Heuristic because the absence of clear
 * events relies on probe coverage: manual em.clear() outside observed
 * events cannot always be attributed.
 */
final class BulkLoadWithoutClearRule implements Rule {

    static final String ID = "bulk-load-without-clear";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        long loads = observedEntityLoads(evidence);
        if (loads <= config.bulkLoadThreshold() || evidence.totalClears() > 0) {
            return List.of();
        }
        return List.of(new Finding(
                ID,
                Severity.MEDIUM,
                Confidence.HEURISTIC,
                format(loads) + " entity loads, 0 clear/evict calls observed",
                "Bulk loading without periodic clear() keeps every loaded "
                        + "entity in the first-level cache until the context "
                        + "closes — memory grows linearly with result size.",
                List.of(
                        "If this is a batch job: flush() + clear() every N "
                                + "entities inside the loop.",
                        "Confirm no lazy association is reloading cleared "
                                + "entities (loads climbing again after clear).",
                        "For read-only bulk paths, StatelessSession avoids "
                                + "the cache entirely.")));
    }

    private static long observedEntityLoads(OperationEvidence evidence) {
        MemoryStatsDelta d = evidence.statsDelta();
        if (d != null && d.entityLoads() != null) {
            return d.entityLoads() + (d.entityFetches() != null ? d.entityFetches() : 0);
        }
        long total = 0;
        boolean any = false;
        for (ContextEvidence c : evidence.contexts()) {
            total += c.entityLoads();
            any = true;
        }
        return any ? total : 0;
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
