package com.institucion.rutas.dominio;

import java.util.List;

/**
 * Resultado inmutable de un cálculo de trayecto.
 *
 * @param origen     id de la sede de origen
 * @param destino    id de la sede de destino
 * @param estado     {@link EstadoRuta#ENCONTRADA} o {@link EstadoRuta#SIN_RUTA}
 * @param ruta       secuencia de sedes desde el origen hasta el destino (vacía si SIN_RUTA)
 * @param costoTotal suma de costos de la ruta, o {@link #SIN_COSTO} si SIN_RUTA
 */
public record ResultadoRuta(String origen, String destino, EstadoRuta estado, List<String> ruta, long costoTotal) {

    /** Valor de costo usado cuando no existe ruta. */
    public static final long SIN_COSTO = -1L;

    public ResultadoRuta {
        ruta = List.copyOf(ruta);
        if (estado == EstadoRuta.SIN_RUTA && (!ruta.isEmpty() || costoTotal != SIN_COSTO)) {
            throw new IllegalArgumentException("Un resultado SIN_RUTA no tiene ruta ni costo");
        }
        if (estado == EstadoRuta.ENCONTRADA && (ruta.isEmpty() || costoTotal < 0)) {
            throw new IllegalArgumentException("Un resultado ENCONTRADA requiere ruta y costo no negativo");
        }
    }

    public static ResultadoRuta encontrada(String origen, String destino, List<String> ruta, long costo) {
        return new ResultadoRuta(origen, destino, EstadoRuta.ENCONTRADA, ruta, costo);
    }

    public static ResultadoRuta sinRuta(String origen, String destino) {
        return new ResultadoRuta(origen, destino, EstadoRuta.SIN_RUTA, List.of(), SIN_COSTO);
    }

    public boolean tieneRuta() {
        return estado == EstadoRuta.ENCONTRADA;
    }

    /** Representación legible, p. ej. {@code A -> C -> B (costo 3)} o {@code SIN_RUTA}. */
    public String describir() {
        if (!tieneRuta()) {
            return "SIN_RUTA: no existe trayecto de " + origen + " a " + destino;
        }
        return String.join(" -> ", ruta) + " (costo " + costoTotal + ")";
    }
}
