package org.institucion.rutas.domain;

public record Site(String name) {
    public Site {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la sede no puede estar vacío.");
        }
        name = name.trim();
    }
}
