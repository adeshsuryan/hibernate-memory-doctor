package io.github.adeshsuryan.hibernatememorydoctor;

/**
 * Optional ORM-level counter deltas measured inside an operation boundary
 * (e.g. Hibernate {@code Statistics} snapshot before and after).
 * Nullable fields mean the source was unavailable and dependent rules
 * stay silent rather than guess.
 */
public record MemoryStatsDelta(
        Long entityLoads,
        Long entityFetches,
        Long collectionLoads,
        Long collectionFetches,
        Long flushes,
        Long queryExecutions) {

    public static MemoryStatsDelta empty() {
        return new MemoryStatsDelta(0L, 0L, 0L, 0L, 0L, 0L);
    }
}
