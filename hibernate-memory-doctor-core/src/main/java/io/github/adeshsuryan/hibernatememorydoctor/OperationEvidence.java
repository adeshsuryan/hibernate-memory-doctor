package io.github.adeshsuryan.hibernatememorydoctor;

import java.util.List;

/**
 * Immutable evidence captured inside one operation boundary.
 * All rules evaluate against this object.
 */
public final class OperationEvidence {

    private final Scope scope;
    private final String label;
    private final boolean readOnly;
    private final List<StatementEvidence> statements;
    private final List<ContextEvidence> contexts;
    private final long wallTimeNanos;
    private final MemoryStatsDelta statsDelta;

    public OperationEvidence(Scope scope, String label, boolean readOnly,
                             List<StatementEvidence> statements,
                             List<ContextEvidence> contexts,
                             long wallTimeNanos, MemoryStatsDelta statsDelta) {
        this.scope = scope;
        this.label = label;
        this.readOnly = readOnly;
        this.statements = List.copyOf(statements);
        this.contexts = List.copyOf(contexts);
        this.wallTimeNanos = wallTimeNanos;
        this.statsDelta = statsDelta;
    }

    public Scope scope() {
        return scope;
    }

    public String label() {
        return label;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public List<StatementEvidence> statements() {
        return statements;
    }

    /** Persistence contexts observed by the probe; empty without Hibernate events. */
    public List<ContextEvidence> contexts() {
        return contexts;
    }

    public long wallTimeNanos() {
        return wallTimeNanos;
    }

    public MemoryStatsDelta statsDelta() {
        return statsDelta;
    }

    public int totalExecutions() {
        int total = 0;
        for (StatementEvidence s : statements) {
            total += s.executions();
        }
        return total;
    }

    /** Every row pulled into the JVM — the raw materialization pressure. */
    public long totalRows() {
        long total = 0;
        for (StatementEvidence s : statements) {
            total += s.rowsReturned();
        }
        return total;
    }

    public long dbTimeNanos() {
        long total = 0;
        for (StatementEvidence s : statements) {
            total += s.totalNanos();
        }
        return total;
    }

    /** Largest managed-entity count observed in any context, or 0 when unprobed. */
    public int peakManagedEntities() {
        int peak = 0;
        for (ContextEvidence c : contexts) {
            peak = Math.max(peak, c.peakManagedEntities());
        }
        return peak;
    }

    /** Entity loads observed by the context probe (independent of ORM stats). */
    public int probedEntityLoads() {
        int total = 0;
        for (ContextEvidence c : contexts) {
            total += c.entityLoads();
        }
        return total;
    }

    /** Clear/evict calls observed across all contexts. */
    public int totalClears() {
        int total = 0;
        for (ContextEvidence c : contexts) {
            total += c.clears();
        }
        return total;
    }
}
