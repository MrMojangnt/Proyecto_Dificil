package com.institucion.rutas.persistencia;

/** Error controlado al leer o escribir archivos de la aplicación. */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
