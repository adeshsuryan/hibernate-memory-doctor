package io.github.adeshsuryan.hibernatememorydoctor;

/**
 * Immutable snapshot of one observed persistence context (Hibernate
 * session / JPA EntityManager) inside an operation boundary. Counts are
 * measured at load events; {@code peakManagedEntities} is the largest
 * first-level-cache size observed, not the sum of everything loaded.
 */
public final class ContextEvidence {

    private final int contextId;
    private final long firstEventNanos;
    private final long lastEventNanos;
    private final int entityLoads;
    private final int collectionLoads;
    private final int clears;
    private final int peakManagedEntities;
    private final int peakCollectionSize;

    ContextEvidence(int contextId, long firstEventNanos, long lastEventNanos,
                    int entityLoads, int collectionLoads, int clears,
                    int peakManagedEntities, int peakCollectionSize) {
        this.contextId = contextId;
        this.firstEventNanos = firstEventNanos;
        this.lastEventNanos = lastEventNanos;
        this.entityLoads = entityLoads;
        this.collectionLoads = collectionLoads;
        this.clears = clears;
        this.peakManagedEntities = peakManagedEntities;
        this.peakCollectionSize = peakCollectionSize;
    }

    /** Identity hash of the observed context object; correlates report lines only. */
    public int contextId() {
        return contextId;
    }

    /** Time between first and last observed event on this context. */
    public long spanNanos() {
        return Math.max(0, lastEventNanos - firstEventNanos);
    }

    public int entityLoads() {
        return entityLoads;
    }

    public int collectionLoads() {
        return collectionLoads;
    }

    /** Session.clear() / Session.evict() calls observed — the batch-loop release valve. */
    public int clears() {
        return clears;
    }

    /** Largest managed-entity count sampled inside this context. */
    public int peakManagedEntities() {
        return peakManagedEntities;
    }

    /** Largest single collection materialized inside this context. */
    public int peakCollectionSize() {
        return peakCollectionSize;
    }
}
