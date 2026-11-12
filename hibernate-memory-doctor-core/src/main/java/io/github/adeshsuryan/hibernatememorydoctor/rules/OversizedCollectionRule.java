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
 * A single persistent collection materialized more elements than the
 * threshold. One fat @OneToMany is the classic deep-graph memory trap.
 * Exact: the collection size was sampled at materialization.
 */
final class OversizedCollectionRule implements Rule {

    static final String ID = "oversized-collection";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (ContextEvidence c : evidence.contexts()) {
            if (c.peakCollectionSize() <= config.collectionSizeThreshold()) {
                continue;
            }
            out.add(new Finding(
                    ID,
                    Severity.MEDIUM,
                    Confidence.EXACT,
                    "collection materialized " + format(c.peakCollectionSize())
                            + " elements in one context",
                    "A collection this size allocates all its elements plus "
                            + "the PersistentCollection wrapper — one parent "
                            + "row carrying an unbounded child set.",
                    List.of(
                            "Find the mapping (usually a @OneToMany with no "
                                    + "@BatchSize / no paging) and query the "
                                    + "children paged instead.",
                            "If only counts/existence are needed, replace the "
                                    + "collection read with a projection query.",
                            "extra-lazy collections (@LazyCollection(EXTRA)) "
                                    + "help for count-only access.")));
        }
        return out;
    }

    private static String format(int n) {
        return String.format("%,d", n);
    }
}
