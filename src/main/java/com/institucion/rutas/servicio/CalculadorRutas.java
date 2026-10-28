package com.institucion.rutas.servicio;

import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.ResultadoRuta;

/** Estrategia de cálculo de trayectos sobre un {@link MapaSedes}. */
public interface CalculadorRutas {

    /**
     * Calcula el trayecto de menor costo.
     *
     * @return resultado con estado {@code ENCONTRADA} o {@code SIN_RUTA}; nunca {@code null}
     * @throws com.institucion.rutas.dominio.SedeInexistenteException si origen o destino no existen
     * @throws com.institucion.rutas.dominio.MapaException            si algún identificador es inválido
     */
    ResultadoRuta calcular(MapaSedes mapa, String origen, String destino);
}
