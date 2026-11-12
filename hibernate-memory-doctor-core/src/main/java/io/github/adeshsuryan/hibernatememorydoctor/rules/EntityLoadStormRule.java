package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Confidence;
import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryStatsDelta;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Severity;
import java.util.List;

/**
 * Entity hydrations in one operation exceeded the budget. Each hydrated
 * entity allocates, and managed ones stay reachable from the context.
 * Exact when ORM statistics supplied the count, heuristic when only the
 * context probe saw the loads.
 */
final class EntityLoadStormRule implements Rule {

    static final String ID = "entity-load-storm";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        MemoryStatsDelta d = evidence.statsDelta();
        Long statsLoads = d != null && d.entityLoads() != null
                ? d.entityLoads() + (d.entityFetches() != null ? d.entityFetches() : 0)
                : null;
        int probeLoads = evidence.probedEntityLoads();
        if (statsLoads == null && probeLoads == 0) {
            return List.of();
        }
        long loads = statsLoads != null ? statsLoads : probeLoads;
        if (loads <= config.entityLoadBudget()) {
            return List.of();
        }
        return List.of(new Finding(
                ID,
                Severity.MEDIUM,
                statsLoads != null ? Confidence.EXACT : Confidence.HEURISTIC,
                format(loads) + " entity hydrations in one operation",
                "Each hydration allocates an entity instance; managed "
                        + "entities stay reachable from the persistence "
                        + "context. Hydration volume this size is usually a "
                        + "fetch graph wider than the caller needs.",
                List.of(
                        "Check which associations were hydrated — entity "
                                + "graphs and EAGER mappings are the usual "
                                + "amplifiers.",
                        "DTO projections skip hydration entirely for "
                                + "read-only paths.",
                        "If paired with row-flood, the same wide statement "
                                + "is producing both signals.")));
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
