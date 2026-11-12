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
 * Collection materializations in one operation exceeded the budget —
 * the signature of a deep fetch graph walking many associations.
 */
final class CollectionLoadHeavyRule implements Rule {

    static final String ID = "collection-load-heavy";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        MemoryStatsDelta d = evidence.statsDelta();
        Long statsLoads = d != null && d.collectionLoads() != null
                ? d.collectionLoads() + (d.collectionFetches() != null ? d.collectionFetches() : 0)
                : null;
        int probeLoads = 0;
        for (ContextEvidence c : evidence.contexts()) {
            probeLoads += c.collectionLoads();
        }
        if (statsLoads == null && probeLoads == 0) {
            return List.of();
        }
        long loads = statsLoads != null ? statsLoads : probeLoads;
        if (loads <= config.collectionLoadBudget()) {
            return List.of();
        }
        return List.of(new Finding(
                ID,
                Severity.LOW,
                statsLoads != null ? Confidence.EXACT : Confidence.HEURISTIC,
                format(loads) + " collections materialized in one operation",
                "Each collection load allocates a wrapper plus its "
                        + "elements — deep graphs amplify both object count "
                        + "and GC pressure.",
                List.of(
                        "Review fetch plans for this path — many small "
                                + "collections often means EAGER-by-default "
                                + "associations.",
                        "@BatchSize reduces statement count but not "
                                + "materialized volume; DTOs skip it entirely.")));
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
