package io.github.adeshsuryan.hibernatememorydoctor;

import io.github.adeshsuryan.hibernatememorydoctor.rules.RuleRegistry;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Positive + negative case per v1 rule over fabricated evidence - no DB.
 */
class RulesTest {

    private static final MemoryDoctorConfig CONFIG = MemoryDoctorConfig.defaults();
    private final RuleRegistry rules = new RuleRegistry();

    private static StatementEvidence stmt(String sql, long nanos, long rows) {
        StatementEvidence s = new StatementEvidence(SqlNormalizer.normalize(sql));
        s.record(nanos);
        s.addRows(rows);
        return s;
    }

    private static ContextEvidence ctx(int peakManaged, int loads, int colls,
                                       int clears, int peakColl, long spanNanos) {
        return new ContextEvidence(1, 0, spanNanos, loads, colls, clears,
                peakManaged, peakColl);
    }

    private static OperationEvidence ev(List<StatementEvidence> statements,
                                        List<ContextEvidence> contexts,
                                        MemoryStatsDelta delta) {
        return new OperationEvidence(Scope.TEST, "op", false, statements,
                contexts, 1_000_000, delta);
    }

    private boolean fires(OperationEvidence e, String ruleId) {
        return rules.evaluate(e, CONFIG).stream()
                .anyMatch(f -> f.ruleId().equals(ruleId));
    }

    @Test
    void persistenceContextGrowth() {
        assertTrue(fires(ev(List.of(), List.of(ctx(6_000, 6_000, 0, 0, 0, 100)), null),
                "persistence-context-growth"));
        assertFalse(fires(ev(List.of(), List.of(ctx(4_999, 4_999, 0, 0, 0, 100)), null),
                "persistence-context-growth"));
        assertFalse(fires(ev(List.of(), List.of(), null),
                "persistence-context-growth"));
    }

    @Test
    void rowFlood() {
        assertTrue(fires(ev(List.of(stmt("select * from t", 10, 11_000)),
                List.of(), null), "row-flood"));
        assertFalse(fires(ev(List.of(stmt("select * from t", 10, 9_999)),
                List.of(), null), "row-flood"));
    }

    @Test
    void bulkLoadWithoutClear() {
        MemoryStatsDelta d = new MemoryStatsDelta(6_000L, 0L, 0L, 0L, 0L, 0L);
        assertTrue(fires(ev(List.of(), List.of(ctx(6_000, 6_000, 0, 0, 0, 100)), d),
                "bulk-load-without-clear"));
        // clears present -> release valve used, stay silent
        assertFalse(fires(ev(List.of(), List.of(ctx(500, 6_000, 0, 50, 0, 100)), d),
                "bulk-load-without-clear"));
        // under threshold
        MemoryStatsDelta small = new MemoryStatsDelta(100L, 0L, 0L, 0L, 0L, 0L);
        assertFalse(fires(ev(List.of(), List.of(ctx(100, 100, 0, 0, 0, 100)), small),
                "bulk-load-without-clear"));
    }

    @Test
    void wideRowLoad() {
        assertTrue(fires(ev(List.of(stmt("select * from big", 10, 1_500)),
                List.of(), null), "wide-row-load"));
        assertFalse(fires(ev(List.of(stmt("select * from big", 10, 999)),
                List.of(), null), "wide-row-load"));
    }

    @Test
    void wideRowLoadDropsSeverityWhenBounded() {
        OperationEvidence e = ev(List.of(
                stmt("select * from big limit ?", 10, 1_500)),
                List.of(), null);
        var finding = rules.evaluate(e, CONFIG).stream()
                .filter(f -> f.ruleId().equals("wide-row-load")).findFirst();
        assertTrue(finding.isPresent());
        assertTrue(finding.get().severity() == Severity.LOW);
    }

    @Test
    void entityLoadStorm() {
        MemoryStatsDelta d = new MemoryStatsDelta(2_500L, 0L, 0L, 0L, 0L, 0L);
        assertTrue(fires(ev(List.of(), List.of(), d), "entity-load-storm"));
        MemoryStatsDelta small = new MemoryStatsDelta(2_000L, 0L, 0L, 0L, 0L, 0L);
        assertFalse(fires(ev(List.of(), List.of(), small), "entity-load-storm"));
        // probe-only path (no stats delta)
        assertTrue(fires(ev(List.of(), List.of(ctx(0, 2_500, 0, 0, 0, 100)), null),
                "entity-load-storm"));
        // both channels absent -> silent
        assertFalse(fires(ev(List.of(), List.of(), null), "entity-load-storm"));
    }

    @Test
    void oversizedCollection() {
        assertTrue(fires(ev(List.of(), List.of(ctx(0, 0, 1, 0, 3_000, 100)), null),
                "oversized-collection"));
        assertFalse(fires(ev(List.of(), List.of(ctx(0, 0, 1, 0, 2_000, 100)), null),
                "oversized-collection"));
    }

    @Test
    void collectionLoadHeavy() {
        MemoryStatsDelta d = new MemoryStatsDelta(0L, 0L, 600L, 0L, 0L, 0L);
        assertTrue(fires(ev(List.of(), List.of(), d), "collection-load-heavy"));
        MemoryStatsDelta small = new MemoryStatsDelta(0L, 0L, 500L, 0L, 0L, 0L);
        assertFalse(fires(ev(List.of(), List.of(), small), "collection-load-heavy"));
    }

    @Test
    void rowExplosion() {
        // 20k rows vs 1k entity loads -> ratio 20 > 10
        MemoryStatsDelta d = new MemoryStatsDelta(1_000L, 0L, 0L, 0L, 0L, 0L);
        assertTrue(fires(ev(List.of(stmt("select * from t", 10, 20_000)),
                List.of(), d), "row-explosion"));
        // balanced ratio -> silent
        assertFalse(fires(ev(List.of(stmt("select * from t", 10, 20_000)),
                List.of(), new MemoryStatsDelta(5_000L, 0L, 0L, 0L, 0L, 0L)),
                "row-explosion"));
        // rows under total budget -> silent regardless of ratio
        assertFalse(fires(ev(List.of(stmt("select * from t", 10, 5_000)),
                List.of(), new MemoryStatsDelta(1L, 0L, 0L, 0L, 0L, 0L)),
                "row-explosion"));
        // no entity-load evidence at all -> silent (guarded, never guesses)
        assertFalse(fires(ev(List.of(stmt("select * from t", 10, 20_000)),
                List.of(), null), "row-explosion"));
    }

    @Test
    void longLivedContext() {
        long sixSecs = 6_000_000_000L;
        assertTrue(fires(ev(List.of(), List.of(ctx(100, 50, 0, 0, 0, sixSecs)), null),
                "long-lived-context"));
        // short span
        assertFalse(fires(ev(List.of(), List.of(ctx(100, 50, 0, 0, 0, 100)), null),
                "long-lived-context"));
        // long span but zero loads -> silent
        assertFalse(fires(ev(List.of(), List.of(ctx(0, 0, 0, 0, 0, sixSecs)), null),
                "long-lived-context"));
    }

    @Test
    void findingsSortedSeverityThenId() {
        MemoryStatsDelta d = new MemoryStatsDelta(6_000L, 0L, 0L, 0L, 0L, 0L);
        OperationEvidence e = ev(
                List.of(stmt("select * from big", 10, 20_000)),
                List.of(ctx(6_000, 6_000, 0, 0, 0, 100)), d);
        var findings = rules.evaluate(e, CONFIG);
        assertFalse(findings.isEmpty());
        for (int i = 1; i < findings.size(); i++) {
            var prev = findings.get(i - 1);
            var cur = findings.get(i);
            assertTrue(prev.severity().ordinal() >= cur.severity().ordinal());
        }
        Predicate<Finding> isExact = f -> f.confidence() == Confidence.EXACT;
        assertTrue(findings.stream().filter(f -> f.ruleId().equals("row-flood"))
                .findFirst().map(isExact::test).orElse(false));
        assertTrue(findings.stream().filter(f -> f.ruleId().equals("bulk-load-without-clear"))
                .findFirst().map(f -> f.confidence() == Confidence.HEURISTIC).orElse(false));
    }
}
