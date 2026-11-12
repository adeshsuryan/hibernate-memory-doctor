package io.github.adeshsuryan.hibernatememorydoctor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One open operation boundary. Statements executed and persistence-context
 * events observed on this thread while the boundary is open are grouped
 * into it. Close always via try-with-resources or {@link MemoryDoctor#check}.
 */
public final class OperationBoundary implements AutoCloseable {

    private final Scope scope;
    private final String label;
    private final boolean readOnly;
    private final long startNanos;
    private final Supplier<MemoryStatsDelta> statsSource;
    private final MemoryStatsDelta statsBefore;
    private final Map<String, StatementEvidence> statements = new LinkedHashMap<>();
    private final Map<Integer, ContextTrace> contexts = new LinkedHashMap<>();
    private volatile OperationEvidence evidence;
    private boolean closed;

    OperationBoundary(Scope scope, String label, boolean readOnly,
                      Supplier<MemoryStatsDelta> statsSource) {
        this.scope = scope;
        this.label = label;
        this.readOnly = readOnly;
        this.statsSource = statsSource;
        this.statsBefore = statsSource != null ? statsSource.get() : null;
        this.startNanos = System.nanoTime();
        Capture.push(this);
    }

    StatementEvidence record(String sql, long nanos) {
        if (closed) {
            return null;
        }
        String normalized = SqlNormalizer.normalize(sql);
        if (normalized.isEmpty()) {
            return null;
        }
        StatementEvidence ev;
        synchronized (statements) {
            ev = statements.computeIfAbsent(normalized, StatementEvidence::new);
        }
        ev.record(nanos);
        return ev;
    }

    void entityLoad(Object context, int managedEntities) {
        trace(context).entityLoad(managedEntities);
    }

    void collectionLoad(Object context, int size) {
        trace(context).collectionLoad(size);
    }

    void clear(Object context) {
        trace(context).cleared();
    }

    private ContextTrace trace(Object context) {
        if (closed) {
            return new ContextTrace(0);
        }
        int id = System.identityHashCode(context);
        synchronized (contexts) {
            return contexts.computeIfAbsent(id, ContextTrace::new);
        }
    }

    /** Close the boundary and finalize the evidence. Idempotent. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            Capture.pop(this);
            MemoryStatsDelta delta = null;
            if (statsSource != null && statsBefore != null) {
                MemoryStatsDelta after = statsSource.get();
                delta = new MemoryStatsDelta(
                        diff(after.entityLoads(), statsBefore.entityLoads()),
                        diff(after.entityFetches(), statsBefore.entityFetches()),
                        diff(after.collectionLoads(), statsBefore.collectionLoads()),
                        diff(after.collectionFetches(), statsBefore.collectionFetches()),
                        diff(after.flushes(), statsBefore.flushes()),
                        diff(after.queryExecutions(), statsBefore.queryExecutions()));
            }
            List<StatementEvidence> statementsSnapshot;
            synchronized (statements) {
                statementsSnapshot = new ArrayList<>(statements.values());
            }
            List<ContextEvidence> contextsSnapshot;
            synchronized (contexts) {
                contextsSnapshot = new ArrayList<>(contexts.size());
                for (ContextTrace t : contexts.values()) {
                    contextsSnapshot.add(t.snapshot());
                }
            }
            evidence = new OperationEvidence(scope, label, readOnly, statementsSnapshot,
                    contextsSnapshot, System.nanoTime() - startNanos, delta);
        }
    }

    private static Long diff(Long after, Long before) {
        if (after == null || before == null) {
            return null;
        }
        return after - before;
    }

    /** Evidence so far; closes the boundary first if still open. */
    public OperationEvidence evidence() {
        if (!closed) {
            close();
        }
        return evidence;
    }
}
