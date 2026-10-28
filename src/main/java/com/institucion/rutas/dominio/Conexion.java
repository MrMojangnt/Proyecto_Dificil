package com.institucion.rutas.dominio;

/**
 * Conexión dirigida entre dos sedes (arista del grafo).
 *
 * @param origen  id de la sede de salida
 * @param destino id de la sede de llegada
 * @param costo   costo entero no negativo
 */
public record Conexion(String origen, String destino, int costo) {

    public Conexion {
        if (costo < 0) {
            throw new PesoInvalidoException(costo);
        }
        origen = Sede.validarId(origen);
        destino = Sede.validarId(destino);
    }
}
