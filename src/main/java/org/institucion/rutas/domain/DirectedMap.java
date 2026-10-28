package org.institucion.rutas.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DirectedMap {
    private final Map<String, Site> sites = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> connections = new LinkedHashMap<>();

    public synchronized void addSite(String name) {
        Site site = new Site(name);
        sites.putIfAbsent(site.name(), site);
        connections.computeIfAbsent(site.name(), ignored -> new LinkedHashMap<>());
    }

    public synchronized void addConnection(String origin, String destination, int cost) {
        Connection connection = new Connection(origin, destination, cost);
        requireSite(connection.origin());
        requireSite(connection.destination());
        connections.get(connection.origin()).put(connection.destination(), connection.cost());
    }

    public synchronized boolean containsSite(String name) {
        return sites.containsKey(name);
    }

    public synchronized List<Site> sites() {
        return List.copyOf(sites.values());
    }

    public synchronized List<Connection> connections() {
        List<Connection> result = new ArrayList<>();
        connections.forEach((origin, outgoing) ->
                outgoing.forEach((destination, cost) -> result.add(new Connection(origin, destination, cost))));
        return List.copyOf(result);
    }

    public synchronized Map<String, Integer> outgoingFrom(String site) {
        requireSite(site);
        return Collections.unmodifiableMap(new LinkedHashMap<>(connections.get(site)));
    }

    private void requireSite(String name) {
        if (!sites.containsKey(Objects.requireNonNull(name))) {
            throw new IllegalArgumentException("La sede no existe: " + name);
        }
    }
}
