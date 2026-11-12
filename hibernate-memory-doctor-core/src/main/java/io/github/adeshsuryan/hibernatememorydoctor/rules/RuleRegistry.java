package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Runs every registered rule over one operation's evidence. */
public final class RuleRegistry {

    private final List<Rule> rules = List.of(
            new PersistenceContextGrowthRule(),
            new BulkLoadWithoutClearRule(),
            new RowFloodRule(),
            new WideRowLoadRule(),
            new EntityLoadStormRule(),
            new OversizedCollectionRule(),
            new CollectionLoadHeavyRule(),
            new RowExplosionRule(),
            new LongLivedContextRule());

    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (Rule rule : rules) {
            out.addAll(rule.evaluate(evidence, config));
        }
        out.sort(Comparator
                .comparing((Finding f) -> f.severity().ordinal())
                .reversed()
                .thenComparing(Finding::ruleId));
        return List.copyOf(out);
    }

    public List<Rule> rules() {
        return rules;
    }
}
