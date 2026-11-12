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
 * A persistence context stayed active longer than the threshold while
 * still accumulating loads. Long-lived contexts (OSIV per-request
 * sessions, manually held Sessions) retain entities for their whole
 * lifespan. Heuristic: the span is measured between observed events, so
 * the true lifetime is at least this long.
 */
final class LongLivedContextRule implements Rule {

    static final String ID = "long-lived-context";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (ContextEvidence c : evidence.contexts()) {
            if (c.spanNanos() <= config.contextSpanNanos()) {
                continue;
            }
            int loads = c.entityLoads() + c.collectionLoads();
            if (loads == 0) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.LOW,
                    Confidence.HEURISTIC,
                    "context active ~" + (c.spanNanos() / 1_000_000_000L)
                            + "s and still loading at the end",
                    "A context that lives this long keeps every entity it "
                            + "loads reachable for its entire lifespan — "
                            + "typical of open-session-in-view or a manually "
                            + "held Session.",
                    List.of(
                            "If OSIV is on, consider disabling it and loading "
                                    + "what the view needs inside the service "
                                    + "transaction.",
                            "Long-running job? ensure the Session is scoped to "
                                    + "a work unit, not the whole job.",
                            "Pair with clear()/evict() discipline in "
                                    + "long-lived contexts.")));
        }
        return out;
    }
}
