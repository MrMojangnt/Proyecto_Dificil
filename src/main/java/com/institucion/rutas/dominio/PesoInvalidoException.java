package com.institucion.rutas.dominio;

/** Se lanza cuando una conexión tiene un costo negativo. */
public class PesoInvalidoException extends MapaException {

    public PesoInvalidoException(long costo) {
        super("Costo inválido " + costo + ": el costo de una conexión debe ser un entero no negativo");
    }
}
