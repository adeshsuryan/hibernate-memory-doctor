package io.github.adeshsuryan.hibernatememorydoctor.junit;

import java.util.List;

/**
 * Limits an operation must stay within. {@link #maxRows(int)} is the one
 * every memory-budget test should pin; the others cover the
 * persistence-context and hydration dimensions.
 */
public final class Budget {

    private final Integer maxRows;
    private final Integer maxManagedEntities;
    private final Integer maxEntityLoads;
    private final Integer maxDbTimeMs;
    private final List<String> forbidRules;

    private Budget(Integer maxRows, Integer maxManagedEntities,
                   Integer maxEntityLoads, Integer maxDbTimeMs,
                   List<String> forbidRules) {
        this.maxRows = maxRows;
        this.maxManagedEntities = maxManagedEntities;
        this.maxEntityLoads = maxEntityLoads;
        this.maxDbTimeMs = maxDbTimeMs;
        this.forbidRules = forbidRules == null ? List.of() : List.copyOf(forbidRules);
    }

    public static Budget maxRows(int value) {
        return new Budget(value, null, null, null, null);
    }

    public static Budget none() {
        return new Budget(null, null, null, null, null);
    }

    public Budget maxManagedEntities(int value) {
        return new Budget(maxRows, value, maxEntityLoads, maxDbTimeMs, forbidRules);
    }

    public Budget maxEntityLoads(int value) {
        return new Budget(maxRows, maxManagedEntities, value, maxDbTimeMs, forbidRules);
    }

    public Budget maxDbTimeMs(int value) {
        return new Budget(maxRows, maxManagedEntities, maxEntityLoads, value, forbidRules);
    }

    public Budget forbidRules(String... ruleIds) {
        return new Budget(maxRows, maxManagedEntities, maxEntityLoads, maxDbTimeMs,
                List.of(ruleIds));
    }

    public Integer maxRows() {
        return maxRows;
    }

    public Integer maxManagedEntities() {
        return maxManagedEntities;
    }

    public Integer maxEntityLoads() {
        return maxEntityLoads;
    }

    public Integer maxDbTimeMs() {
        return maxDbTimeMs;
    }

    public List<String> forbidRules() {
        return forbidRules;
    }
}
