package io.github.adeshsuryan.hibernatememorydoctor.rules;

/** Shared predicates for statement-shape checks. */
final class Statements {

    private Statements() {
    }

    /** True when the statement carries an explicit row bound. */
    static boolean hasPagination(String normalizedSql) {
        return normalizedSql.contains(" limit ")
                || normalizedSql.contains(" fetch first ")
                || normalizedSql.contains(" top ")
                || normalizedSql.contains(" rownum ")
                || normalizedSql.contains(" for update skip locked");
    }
}
