package io.github.adeshsuryan.hibernatememorydoctor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SqlNormalizerTest {

    @Test
    void stripsStringAndNumericLiterals() {
        String normalized = SqlNormalizer.normalize(
                "SELECT * FROM customer WHERE name = 'Brien' AND id = 42");
        assertEquals("select * from customer where name = ? and id = ?",
                normalized);
    }

    @Test
    void literalsNeverReachOutput() {
        String normalized = SqlNormalizer.normalize(
                "select * from t where email = 'pii@example.com' and ssn='123-45'");
        assertFalse(normalized.contains("pii"));
        assertFalse(normalized.contains("123"));
    }

    @Test
    void collapsesWhitespaceAndCase() {
        assertEquals("select * from t where a = ?",
                SqlNormalizer.normalize("  SELECT   *   FROM t WHERE a = 5  "));
    }
}
