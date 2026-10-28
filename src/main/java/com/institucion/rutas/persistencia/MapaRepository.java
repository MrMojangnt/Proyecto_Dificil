package com.institucion.rutas.persistencia;

import com.institucion.rutas.dominio.MapaSedes;

/** Almacenamiento del mapa vigente. */
public interface MapaRepository {

    /** @return el mapa guardado, o un mapa vacío si todavía no hay datos */
    MapaSedes cargar();

    void guardar(MapaSedes mapa);
}
