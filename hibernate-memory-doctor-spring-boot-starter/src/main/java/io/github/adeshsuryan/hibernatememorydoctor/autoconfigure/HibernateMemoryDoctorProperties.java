package io.github.adeshsuryan.hibernatememorydoctor.autoconfigure;

import io.github.adeshsuryan.hibernatememorydoctor.MemoryDoctorConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for hibernate-memory-doctor. All thresholds are
 * per-operation. Bind-parameter values are never captured regardless of
 * settings.
 */
@ConfigurationProperties(prefix = "hibernate-memory-doctor")
public class HibernateMemoryDoctorProperties {

    /** Master switch. When false nothing is captured and the DataSource is not wrapped. */
    private boolean enabled = true;

    /** Managed entities in one persistence context before persistence-context-growth fires. */
    private int contextGrowthThreshold = MemoryDoctorConfig.DEFAULT_CONTEXT_GROWTH_THRESHOLD;

    /** Entity loads with zero clear/evict calls before bulk-load-without-clear fires. */
    private int bulkLoadThreshold = MemoryDoctorConfig.DEFAULT_BULK_LOAD_THRESHOLD;

    /** Total rows fetched per operation before row-flood fires. */
    private long totalRowsBudget = MemoryDoctorConfig.DEFAULT_TOTAL_ROWS_BUDGET;

    /** Rows returned by one statement before wide-row-load fires. */
    private long wideRowThreshold = MemoryDoctorConfig.DEFAULT_WIDE_ROW_THRESHOLD;

    /** Entity hydrations per operation before entity-load-storm fires. */
    private long entityLoadBudget = MemoryDoctorConfig.DEFAULT_ENTITY_LOAD_BUDGET;

    /** Collection loads per operation before collection-load-heavy fires. */
    private long collectionLoadBudget = MemoryDoctorConfig.DEFAULT_COLLECTION_LOAD_BUDGET;

    /** Elements in one collection before oversized-collection fires. */
    private int collectionSizeThreshold = MemoryDoctorConfig.DEFAULT_COLLECTION_SIZE_THRESHOLD;

    /** rows : entity-loads ratio before row-explosion fires. */
    private int rowExplosionFactor = MemoryDoctorConfig.DEFAULT_ROW_EXPLOSION_FACTOR;

    /** Persistence-context lifetime in milliseconds before long-lived-context fires. */
    private long contextSpanMs = MemoryDoctorConfig.DEFAULT_CONTEXT_SPAN_NANOS / 1_000_000;

    /** Request-scope reporting: log-on-findings, log-always, or off. */
    private String reportMode = "log-on-findings";

    /** Hibernate event-listener probe for persistence-context sampling. */
    private boolean probe = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getContextGrowthThreshold() {
        return contextGrowthThreshold;
    }

    public void setContextGrowthThreshold(int v) {
        this.contextGrowthThreshold = v;
    }

    public int getBulkLoadThreshold() {
        return bulkLoadThreshold;
    }

    public void setBulkLoadThreshold(int v) {
        this.bulkLoadThreshold = v;
    }

    public long getTotalRowsBudget() {
        return totalRowsBudget;
    }

    public void setTotalRowsBudget(long v) {
        this.totalRowsBudget = v;
    }

    public long getWideRowThreshold() {
        return wideRowThreshold;
    }

    public void setWideRowThreshold(long v) {
        this.wideRowThreshold = v;
    }

    public long getEntityLoadBudget() {
        return entityLoadBudget;
    }

    public void setEntityLoadBudget(long v) {
        this.entityLoadBudget = v;
    }

    public long getCollectionLoadBudget() {
        return collectionLoadBudget;
    }

    public void setCollectionLoadBudget(long v) {
        this.collectionLoadBudget = v;
    }

    public int getCollectionSizeThreshold() {
        return collectionSizeThreshold;
    }

    public void setCollectionSizeThreshold(int v) {
        this.collectionSizeThreshold = v;
    }

    public int getRowExplosionFactor() {
        return rowExplosionFactor;
    }

    public void setRowExplosionFactor(int v) {
        this.rowExplosionFactor = v;
    }

    public long getContextSpanMs() {
        return contextSpanMs;
    }

    public void setContextSpanMs(long v) {
        this.contextSpanMs = v;
    }

    public String getReportMode() {
        return reportMode;
    }

    public void setReportMode(String reportMode) {
        this.reportMode = reportMode;
    }

    public boolean isProbe() {
        return probe;
    }

    public void setProbe(boolean probe) {
        this.probe = probe;
    }
}
