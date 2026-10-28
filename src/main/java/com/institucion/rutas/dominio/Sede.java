package com.institucion.rutas.dominio;

import java.util.regex.Pattern;

/**
 * Sede de la institución (nodo del grafo).
 *
 * @param id     identificador único: letras, dígitos, '_' o '-' (1 a 40 caracteres)
 * @param nombre nombre descriptivo; si se omite se usa el id
 */
public record Sede(String id, String nombre) {

    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9_-]{1,40}");

    public Sede {
        id = validarId(id);
        if (nombre == null || nombre.isBlank()) {
            nombre = id;
        }
        nombre = nombre.strip();
        if (nombre.contains(";") || nombre.contains("\n") || nombre.contains("\r")) {
            throw new MapaException("El nombre de la sede no puede contener ';' ni saltos de línea");
        }
    }

    public Sede(String id) {
        this(id, null);
    }

    /** Normaliza y valida un identificador de sede. */
    public static String validarId(String id) {
        if (id == null || id.isBlank()) {
            throw new MapaException("El identificador de la sede es obligatorio");
        }
        String limpio = id.strip();
        if (!ID_VALIDO.matcher(limpio).matches()) {
            throw new MapaException("Identificador de sede inválido '" + limpio
                    + "': use 1 a 40 letras, dígitos, '_' o '-'");
        }
        return limpio;
    }
}
