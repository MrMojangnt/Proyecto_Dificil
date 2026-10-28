package org.institucion.rutas.service;

import org.institucion.rutas.domain.DirectedMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public final class RouteService {
    private final DirectedMap map;

    public RouteService(DirectedMap map) {
        this.map = map;
    }

    public RouteResult shortestRoute(String origin, String destination) {
        if (!map.containsSite(origin)) {
            throw new IllegalArgumentException("La sede de origen no existe: " + origin);
        }
        if (!map.containsSite(destination)) {
            throw new IllegalArgumentException("La sede de destino no existe: " + destination);
        }
        if (origin.equals(destination)) {
            return new RouteResult(RouteStatus.OK, java.util.List.of(origin), 0);
        }

        Map<String, Long> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(
                java.util.Comparator.comparingLong(QueueEntry::distance));
        distances.put(origin, 0L);
        queue.add(new QueueEntry(origin, 0L));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            if (current.distance() != distances.getOrDefault(current.site(), Long.MAX_VALUE)) {
                continue;
            }
            if (current.site().equals(destination)) {
                break;
            }
            for (Map.Entry<String, Integer> edge : map.outgoingFrom(current.site()).entrySet()) {
                long candidate = current.distance() + edge.getValue();
                if (candidate < distances.getOrDefault(edge.getKey(), Long.MAX_VALUE)) {
                    distances.put(edge.getKey(), candidate);
                    previous.put(edge.getKey(), current.site());
                    queue.add(new QueueEntry(edge.getKey(), candidate));
                }
            }
        }

        Long totalCost = distances.get(destination);
        if (totalCost == null) {
            return new RouteResult(RouteStatus.SIN_RUTA, java.util.List.of(), 0);
        }

        ArrayList<String> route = new ArrayList<>();
        for (String site = destination; site != null; site = previous.get(site)) {
            route.add(site);
            if (site.equals(origin)) {
                break;
            }
        }
        if (!route.get(route.size() - 1).equals(origin)) {
            throw new IllegalStateException("No se pudo reconstruir una ruta válida.");
        }
        Collections.reverse(route);
        return new RouteResult(RouteStatus.OK, route, totalCost);
    }

    private record QueueEntry(String site, long distance) {
    }
}
