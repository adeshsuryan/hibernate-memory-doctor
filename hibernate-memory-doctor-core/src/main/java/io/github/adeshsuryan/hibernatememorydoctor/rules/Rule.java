package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import java.util.List;

/**
 * One diagnostic rule. Implementations must be deterministic over the same
 * {@link OperationEvidence} and must never throw on missing or partial
 * evidence — silence is better than a false positive.
 */
public interface Rule {

    /** Frozen rule id. Once published, an id never changes meaning. */
    String id();

    List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config);
}
