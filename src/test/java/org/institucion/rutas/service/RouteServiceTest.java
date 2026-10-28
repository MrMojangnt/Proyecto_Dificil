package org.institucion.rutas.service;

import org.institucion.rutas.domain.DirectedMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteServiceTest {
    @Test
    void choosesCheaperIndirectRouteAndReconstructsIt() {
        DirectedMap map = mapWithSites("A", "B", "C");
        map.addConnection("A", "B", 4);
        map.addConnection("A", "C", 1);
        map.addConnection("C", "B", 2);

        RouteResult result = new RouteService(map).shortestRoute("A", "B");

        assertEquals(RouteStatus.OK, result.status());
        assertEquals(java.util.List.of("A", "C", "B"), result.route());
        assertEquals(3L, result.totalCost());
    }

    @Test
    void isolatedDestinationReturnsNoRoute() {
        DirectedMap map = mapWithSites("A", "B");
        RouteResult result = new RouteService(map).shortestRoute("A", "B");
        assertEquals(RouteStatus.SIN_RUTA, result.status());
        assertTrue(result.route().isEmpty());
    }

    @Test
    void sameOriginAndDestinationCostsZero() {
        DirectedMap map = mapWithSites("A");
        RouteResult result = new RouteService(map).shortestRoute("A", "A");
        assertEquals(RouteStatus.OK, result.status());
        assertEquals(java.util.List.of("A"), result.route());
        assertEquals(0, result.totalCost());
    }

    @Test
    void unknownOriginIsRejected() {
        DirectedMap map = mapWithSites("B");
        assertThrows(IllegalArgumentException.class, () -> new RouteService(map).shortestRoute("A", "B"));
    }

    @Test
    void unknownDestinationIsRejected() {
        DirectedMap map = mapWithSites("A");
        assertThrows(IllegalArgumentException.class, () -> new RouteService(map).shortestRoute("A", "B"));
    }

    @Test
    void routesRespectDirection() {
        DirectedMap map = mapWithSites("A", "B");
        map.addConnection("A", "B", 1);
        assertEquals(RouteStatus.SIN_RUTA, new RouteService(map).shortestRoute("B", "A").status());
    }

    @Test
    void usesLongForPathTotals() {
        DirectedMap map = mapWithSites("A", "B", "C");
        map.addConnection("A", "B", Integer.MAX_VALUE);
        map.addConnection("B", "C", Integer.MAX_VALUE);
        assertEquals(4_294_967_294L, new RouteService(map).shortestRoute("A", "C").totalCost());
    }

    @Test
    void zeroCostConnectionsAreSupported() {
        DirectedMap map = mapWithSites("A", "B");
        map.addConnection("A", "B", 0);
        assertEquals(0, new RouteService(map).shortestRoute("A", "B").totalCost());
    }

    @Test
    void ignoresStalePriorityQueueEntries() {
        DirectedMap map = mapWithSites("A", "B", "C", "D");
        map.addConnection("A", "B", 10);
        map.addConnection("A", "C", 1);
        map.addConnection("C", "B", 1);
        map.addConnection("B", "D", 1);
        assertEquals(java.util.List.of("A", "C", "B", "D"),
                new RouteService(map).shortestRoute("A", "D").route());
    }

    private static DirectedMap mapWithSites(String... names) {
        DirectedMap map = new DirectedMap();
        for (String name : names) {
            map.addSite(name);
        }
        return map;
    }
}
