package org.institucion.rutas.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DirectedMapTest {
    @Test
    void rejectsNegativeWeightWithoutAddingConnection() {
        DirectedMap map = new DirectedMap();
        map.addSite("A");
        map.addSite("B");
        assertThrows(IllegalArgumentException.class, () -> map.addConnection("A", "B", -1));
        assertTrue(map.connections().isEmpty());
    }

    @Test
    void rejectsConnectionWithMissingOrigin() {
        DirectedMap map = new DirectedMap();
        map.addSite("B");
        assertThrows(IllegalArgumentException.class, () -> map.addConnection("A", "B", 1));
    }

    @Test
    void rejectsConnectionWithMissingDestination() {
        DirectedMap map = new DirectedMap();
        map.addSite("A");
        assertThrows(IllegalArgumentException.class, () -> map.addConnection("A", "B", 1));
    }

    @Test
    void trimsSiteNamesAndAvoidsDuplicates() {
        DirectedMap map = new DirectedMap();
        map.addSite(" A ");
        map.addSite("A");
        assertEquals(1, map.sites().size());
        assertEquals("A", map.sites().get(0).name());
    }

    @Test
    void rejectsBlankSiteName() {
        DirectedMap map = new DirectedMap();
        assertThrows(IllegalArgumentException.class, () -> map.addSite("  "));
    }

    @Test
    void latestConnectionCostReplacesPreviousCost() {
        DirectedMap map = new DirectedMap();
        map.addSite("A");
        map.addSite("B");
        map.addConnection("A", "B", 8);
        map.addConnection("A", "B", 3);
        assertEquals(3, map.connections().get(0).cost());
    }
}
