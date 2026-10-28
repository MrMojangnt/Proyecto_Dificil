package com.institucion.rutas.persistencia;

import com.institucion.rutas.dominio.MapaSedes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Guarda el mapa en un archivo de texto con {@link FormatoMapa}. */
public final class ArchivoMapaRepository implements MapaRepository {

    private final Path archivo;

    public ArchivoMapaRepository(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo);
    }

    @Override
    public MapaSedes cargar() {
        if (!Files.exists(archivo)) {
            return new MapaSedes();
        }
        return FormatoMapa.leer(archivo);
    }

    @Override
    public void guardar(MapaSedes mapa) {
        FormatoMapa.escribir(mapa, archivo);
    }

    public Path archivo() {
        return archivo;
    }
}
