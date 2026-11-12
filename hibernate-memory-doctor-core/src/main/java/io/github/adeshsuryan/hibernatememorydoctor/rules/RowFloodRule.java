package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Confidence;
import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Severity;
import java.util.List;

/**
 * Total rows pulled into the JVM exceeded the budget. Rows materialize as
 * entities, DTOs, or Object[] — all heap. Exact: rows were counted at the
 * ResultSet boundary.
 */
final class RowFloodRule implements Rule {

    static final String ID = "row-flood";

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
        return List.of(new Finding(
                ID,
                Severity.HIGH,
                Confidence.EXACT,
                format(rows) + " rows fetched across "
                        + evidence.totalExecutions() + " statements",
                "Every fetched row is materialized into heap objects "
                        + "(entities, DTOs, or Object[]). Volume this size is "
                        + "what turns a query into an OutOfMemoryError.",
                List.of(
                        "Add pagination (LIMIT/offset or keyset) — check the "
                                + "widest statements in the report.",
                        "If this is intentional bulk processing, stream with a "
                                + "cursor/ScrollableResults or fetch in chunks.",
                        "Projections? select only the columns you need — wide "
                                + "entity graphs cost more per row.")));
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
