package io.github.adeshsuryan.hibernatememorydoctor.rules;

import io.github.adeshsuryan.hibernatememorydoctor.Confidence;
import io.github.adeshsuryan.hibernatememorydoctor.Finding;
import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import io.github.adeshsuryan.hibernatememorydoctor.OperationEvidence;
import io.github.adeshsuryan.hibernatememorydoctor.Severity;
import io.github.adeshsuryan.hibernatememorydoctor.StatementEvidence;
import java.util.ArrayList;
import java.util.List;

/**
 * One statement returned more rows than the threshold — the single-query
 * memory cliff, usually a missing LIMIT on a search/list endpoint or a
 * fetch-join multiplying rows. Exact row count per statement shape.
 */
final class WideRowLoadRule implements Rule {

    static final String ID = "wide-row-load";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public List<Finding> evaluate(OperationEvidence evidence, MemoryDoctorConfig config) {
        List<Finding> out = new ArrayList<>();
        for (StatementEvidence s : evidence.statements()) {
            if (s.rowsReturned() <= config.wideRowThreshold()) {
                continue;
            }
            boolean bounded = Statements.hasPagination(s.normalizedSql());
            out.add(new Finding(
                    ID,
                    bounded ? Severity.LOW : Severity.MEDIUM,
                    Confidence.EXACT,
                    s.shortSql(90) + "  -> " + format(s.rowsReturned()) + " rows",
                    "A single statement materialized "
                            + format(s.rowsReturned())
                            + " rows on the heap"
                            + (bounded ? " despite an explicit row bound" : " with no row bound")
                            + ".",
                    List.of(
                            bounded
                                    ? "The bound is too high for memory safety — "
                                            + "lower the page size or stream."
                                    : "Add LIMIT / fetch first / pageable — or "
                                            + "check whether a fetch-join multiplied rows.",
                            "Compare rows to entities loaded: a large gap means "
                                    + "join duplication or scalar reads.",
                            "Confirm the caller needs all rows — exporting? "
                                    + "stream instead of materializing a List.")));
        }
        return out;
    }

    private static String format(long n) {
        return String.format("%,d", n);
    }
}
