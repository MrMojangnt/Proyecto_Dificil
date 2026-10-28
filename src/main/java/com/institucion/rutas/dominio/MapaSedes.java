package com.institucion.rutas.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mapa dirigido de sedes. Entre un par ordenado (origen, destino) existe como
 * máximo una conexión; volver a agregarla reemplaza su costo.
 *
 * <p>Todas las operaciones están sincronizadas: la instancia puede compartirse
 * entre hilos. Las validaciones se hacen antes de modificar el estado, de modo
 * que una operación rechazada nunca deja el mapa a medias.</p>
 */
public final class MapaSedes {

    private final Map<String, Sede> sedes = new LinkedHashMap<>();
    /** origen -> (destino -> costo). Toda sede tiene entrada, aunque esté aislada. */
    private final Map<String, Map<String, Integer>> adyacencia = new LinkedHashMap<>();

    public synchronized void agregarSede(Sede sede) {
        if (sedes.containsKey(sede.id())) {
            throw new MapaException("La sede '" + sede.id() + "' ya existe");
        }
        sedes.put(sede.id(), sede);
        adyacencia.put(sede.id(), new LinkedHashMap<>());
    }

    /** Elimina la sede junto con todas sus conexiones entrantes y salientes. */
    public synchronized void eliminarSede(String id) {
        String clave = requerirSede(id);
        sedes.remove(clave);
        adyacencia.remove(clave);
        adyacencia.values().forEach(destinos -> destinos.remove(clave));
    }

    /**
     * Agrega (o reemplaza) la conexión dirigida origen→destino.
     *
     * @throws PesoInvalidoException     si el costo es negativo
     * @throws SedeInexistenteException  si alguna de las sedes no existe
     */
    public synchronized void agregarConexion(Conexion conexion) {
        requerirSede(conexion.origen());
        requerirSede(conexion.destino());
        adyacencia.get(conexion.origen()).put(conexion.destino(), conexion.costo());
    }

    public void agregarConexion(String origen, String destino, int costo) {
        agregarConexion(new Conexion(origen, destino, costo));
    }

    /** @return {@code true} si la conexión existía y fue eliminada */
    public synchronized boolean eliminarConexion(String origen, String destino) {
        String o = requerirSede(origen);
        String d = requerirSede(destino);
        return adyacencia.get(o).remove(d) != null;
    }

    public synchronized boolean contieneSede(String id) {
        return id != null && sedes.containsKey(id.strip());
    }

    public synchronized Optional<Sede> buscarSede(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(sedes.get(id.strip()));
    }

    public synchronized Optional<Integer> costoConexion(String origen, String destino) {
        Map<String, Integer> destinos = adyacencia.get(origen);
        return destinos == null ? Optional.empty() : Optional.ofNullable(destinos.get(destino));
    }

    public synchronized List<Sede> sedes() {
        return List.copyOf(sedes.values());
    }

    public synchronized List<Conexion> conexiones() {
        List<Conexion> todas = new ArrayList<>();
        adyacencia.forEach((origen, destinos) ->
                destinos.forEach((destino, costo) -> todas.add(new Conexion(origen, destino, costo))));
        return Collections.unmodifiableList(todas);
    }

    public synchronized int cantidadSedes() {
        return sedes.size();
    }

    public synchronized int cantidadConexiones() {
        return adyacencia.values().stream().mapToInt(Map::size).sum();
    }

    /**
     * Instantánea inmutable de la lista de adyacencia, apta para que un algoritmo
     * la recorra sin mantener el bloqueo del mapa.
     */
    public synchronized Map<String, List<Conexion>> adyacencia() {
        Map<String, List<Conexion>> copia = new LinkedHashMap<>();
        adyacencia.forEach((origen, destinos) -> {
            List<Conexion> salientes = new ArrayList<>(destinos.size());
            destinos.forEach((destino, costo) -> salientes.add(new Conexion(origen, destino, costo)));
            copia.put(origen, List.copyOf(salientes));
        });
        return Collections.unmodifiableMap(copia);
    }

    /** Copia profunda e independiente del mapa. */
    public synchronized MapaSedes copia() {
        MapaSedes copia = new MapaSedes();
        sedes.values().forEach(copia::agregarSede);
        adyacencia.forEach((origen, destinos) ->
                copia.adyacencia.get(origen).putAll(destinos));
        return copia;
    }

    private String requerirSede(String id) {
        String clave = Sede.validarId(id);
        if (!sedes.containsKey(clave)) {
            throw new SedeInexistenteException(clave);
        }
        return clave;
    }
}
