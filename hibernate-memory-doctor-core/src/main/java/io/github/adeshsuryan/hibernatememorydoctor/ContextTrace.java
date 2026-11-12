package io.github.adeshsuryan.hibernatememorydoctor;

/**
 * Mutable per-context accumulator owned by one {@link OperationBoundary}.
 * Keyed by identity hash — context objects are never referenced, so a
 * closed persistence context cannot be kept alive by the capture layer.
 */
final class ContextTrace {

    private final int contextId;
    private long firstNanos = -1;
    private long lastNanos = -1;
    private int entityLoads;
    private int collectionLoads;
    private int clears;
    private int peakManagedEntities;
    private int peakCollectionSize;

    ContextTrace(int contextId) {
        this.contextId = contextId;
    }

    private void stamp() {
        long now = System.nanoTime();
        if (firstNanos < 0) {
            firstNanos = now;
        }
        lastNanos = now;
    }

    synchronized void entityLoad(int managedEntities) {
        stamp();
        entityLoads++;
        if (managedEntities > peakManagedEntities) {
            peakManagedEntities = managedEntities;
        }
    }

    synchronized void collectionLoad(int size) {
        stamp();
        collectionLoads++;
        if (size > peakCollectionSize) {
            peakCollectionSize = size;
        }
    }

    synchronized void cleared() {
        stamp();
        clears++;
    }

    synchronized ContextEvidence snapshot() {
        return new ContextEvidence(contextId, firstNanos, lastNanos, entityLoads,
                collectionLoads, clears, peakManagedEntities, peakCollectionSize);
    }
}
