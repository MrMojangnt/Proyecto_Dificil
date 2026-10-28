package com.institucion.rutas.persistencia;

/** Contenido de archivo mal formado o inválido, con el número de línea afectado. */
public class FormatoArchivoException extends PersistenciaException {

    private final int linea;

    public FormatoArchivoException(String origen, int linea, String detalle) {
        super(origen + ", línea " + linea + ": " + detalle);
        this.linea = linea;
    }

    public int getLinea() {
        return linea;
    }
}
