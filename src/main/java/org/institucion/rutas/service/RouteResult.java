package org.institucion.rutas.service;

import java.util.List;

public record RouteResult(RouteStatus status, List<String> route, long totalCost) {
    public RouteResult {
        route = List.copyOf(route);
        if (status == RouteStatus.SIN_RUTA && (!route.isEmpty() || totalCost != 0)) {
            throw new IllegalArgumentException("SIN_RUTA debe tener ruta vacía y costo cero.");
        }
        if (status == RouteStatus.OK && route.isEmpty()) {
            throw new IllegalArgumentException("Una ruta válida debe contener al menos una sede.");
        }
    }
}
