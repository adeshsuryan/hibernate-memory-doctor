package io.github.adeshsuryan.hibernatememorydoctor;

/**
 * Thresholds and toggles for rule evaluation. All values have documented
 * defaults; override via the builder or Spring properties.
 */
public final class MemoryDoctorConfig {

    /** First-level-cache size in one context before persistence-context-growth fires. */
    public static final int DEFAULT_CONTEXT_GROWTH_THRESHOLD = 5_000;
    /** Entity loads with zero clears before bulk-load-without-clear fires. */
    public static final int DEFAULT_BULK_LOAD_THRESHOLD = 5_000;
    /** Total rows fetched per operation before row-flood fires. */
    public static final long DEFAULT_TOTAL_ROWS_BUDGET = 10_000;
    /** Rows from one statement before wide-row-load fires. */
    public static final long DEFAULT_WIDE_ROW_THRESHOLD = 1_000;
    /** Entity loads per operation before entity-load-storm fires. */
    public static final long DEFAULT_ENTITY_LOAD_BUDGET = 2_000;
    /** Collection loads per operation before collection-load-heavy fires. */
    public static final long DEFAULT_COLLECTION_LOAD_BUDGET = 500;
    /** Elements in one materialized collection before oversized-collection fires. */
    public static final int DEFAULT_COLLECTION_SIZE_THRESHOLD = 2_000;
    /** rows : entity-loads ratio before row-explosion fires. */
    public static final int DEFAULT_ROW_EXPLOSION_FACTOR = 10;
    /** Context lifetime before long-lived-context fires. */
    public static final long DEFAULT_CONTEXT_SPAN_NANOS = 5_000_000_000L; // 5 s

    private final int contextGrowthThreshold;
    private final int bulkLoadThreshold;
    private final long totalRowsBudget;
    private final long wideRowThreshold;
    private final long entityLoadBudget;
    private final long collectionLoadBudget;
    private final int collectionSizeThreshold;
    private final int rowExplosionFactor;
    private final long contextSpanNanos;

    private MemoryDoctorConfig(Builder b) {
        this.contextGrowthThreshold = b.contextGrowthThreshold;
        this.bulkLoadThreshold = b.bulkLoadThreshold;
        this.totalRowsBudget = b.totalRowsBudget;
        this.wideRowThreshold = b.wideRowThreshold;
        this.entityLoadBudget = b.entityLoadBudget;
        this.collectionLoadBudget = b.collectionLoadBudget;
        this.collectionSizeThreshold = b.collectionSizeThreshold;
        this.rowExplosionFactor = b.rowExplosionFactor;
        this.contextSpanNanos = b.contextSpanNanos;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MemoryDoctorConfig defaults() {
        return builder().build();
    }

    public int contextGrowthThreshold() {
        return contextGrowthThreshold;
    }

    public int bulkLoadThreshold() {
        return bulkLoadThreshold;
    }

    public long totalRowsBudget() {
        return totalRowsBudget;
    }

    public long wideRowThreshold() {
        return wideRowThreshold;
    }

    public long entityLoadBudget() {
        return entityLoadBudget;
    }

    public long collectionLoadBudget() {
        return collectionLoadBudget;
    }

    public int collectionSizeThreshold() {
        return collectionSizeThreshold;
    }

    public int rowExplosionFactor() {
        return rowExplosionFactor;
    }

    public long contextSpanNanos() {
        return contextSpanNanos;
    }

    public static final class Builder {
        private int contextGrowthThreshold = DEFAULT_CONTEXT_GROWTH_THRESHOLD;
        private int bulkLoadThreshold = DEFAULT_BULK_LOAD_THRESHOLD;
        private long totalRowsBudget = DEFAULT_TOTAL_ROWS_BUDGET;
        private long wideRowThreshold = DEFAULT_WIDE_ROW_THRESHOLD;
        private long entityLoadBudget = DEFAULT_ENTITY_LOAD_BUDGET;
        private long collectionLoadBudget = DEFAULT_COLLECTION_LOAD_BUDGET;
        private int collectionSizeThreshold = DEFAULT_COLLECTION_SIZE_THRESHOLD;
        private int rowExplosionFactor = DEFAULT_ROW_EXPLOSION_FACTOR;
        private long contextSpanNanos = DEFAULT_CONTEXT_SPAN_NANOS;

        public Builder contextGrowthThreshold(int v) {
            this.contextGrowthThreshold = v;
            return this;
        }

        public Builder bulkLoadThreshold(int v) {
            this.bulkLoadThreshold = v;
            return this;
        }

        public Builder totalRowsBudget(long v) {
            this.totalRowsBudget = v;
            return this;
        }

        public Builder wideRowThreshold(long v) {
            this.wideRowThreshold = v;
            return this;
        }

        public Builder entityLoadBudget(long v) {
            this.entityLoadBudget = v;
            return this;
        }

        public Builder collectionLoadBudget(long v) {
            this.collectionLoadBudget = v;
            return this;
        }

        public Builder collectionSizeThreshold(int v) {
            this.collectionSizeThreshold = v;
            return this;
        }

        public Builder rowExplosionFactor(int v) {
            this.rowExplosionFactor = v;
            return this;
        }

        public Builder contextSpanNanos(long v) {
            this.contextSpanNanos = v;
            return this;
        }

        public MemoryDoctorConfig build() {
            return new MemoryDoctorConfig(this);
        }
    }
}
