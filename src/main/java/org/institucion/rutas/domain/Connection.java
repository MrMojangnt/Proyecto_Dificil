package org.institucion.rutas.domain;

public record Connection(String origin, String destination, int cost) {
    public Connection {
        origin = new Site(origin).name();
        destination = new Site(destination).name();
        if (cost < 0) {
            throw new IllegalArgumentException("El costo de una conexión no puede ser negativo.");
        }
    }
}
