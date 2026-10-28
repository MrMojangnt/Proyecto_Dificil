package com.institucion.rutas.dominio;

/**
 * Error de negocio controlado. Todos los errores de validación del dominio
 * heredan de esta clase para que la capa de presentación los muestre sin trazas.
 */
public class MapaException extends RuntimeException {

    public MapaException(String mensaje) {
        super(mensaje);
    }

    public MapaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
