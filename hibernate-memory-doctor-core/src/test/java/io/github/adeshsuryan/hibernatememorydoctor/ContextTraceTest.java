package io.github.adeshsuryan.hibernatememorydoctor;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextTraceTest {

    @Test
    void tracksPeakManagedEntitiesNotTotal() {
        ContextTrace t = new ContextTrace(42);
        t.entityLoad(10);
        t.entityLoad(500);
        t.entityLoad(120);
        ContextEvidence ev = t.snapshot();
        assertEquals(500, ev.peakManagedEntities());
        assertEquals(3, ev.entityLoads());
    }

    @Test
    void tracksCollectionPeakAndClears() {
        ContextTrace t = new ContextTrace(7);
        t.collectionLoad(2_000);
        t.collectionLoad(50);
        t.cleared();
        t.cleared();
        ContextEvidence ev = t.snapshot();
        assertEquals(2_000, ev.peakCollectionSize());
        assertEquals(2, ev.collectionLoads());
        assertEquals(2, ev.clears());
    }

    @Test
    void spanIsNonNegative() {
        ContextTrace t = new ContextTrace(9);
        t.entityLoad(1);
        ContextEvidence ev = t.snapshot();
        assertTrue(ev.spanNanos() >= 0);
    }

    @Test
    void negativeManagedSampleDoesNotLowerPeak() {
        ContextTrace t = new ContextTrace(5);
        t.entityLoad(-1);   // probe could not sample
        ContextEvidence ev = t.snapshot();
        assertEquals(0, ev.peakManagedEntities());
        assertEquals(1, ev.entityLoads());
    }
}
