package com.institucion.rutas.dominio;

/** Se lanza cuando se referencia una sede que no existe en el mapa. */
public class SedeInexistenteException extends MapaException {

    private final String idSede;

    public SedeInexistenteException(String idSede) {
        super("La sede '" + idSede + "' no existe en el mapa");
        this.idSede = idSede;
    }

    public String getIdSede() {
        return idSede;
    }
}
