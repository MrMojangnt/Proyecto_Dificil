package com.institucion.rutas.servicio;

import com.institucion.rutas.dominio.Conexion;
import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.ResultadoRuta;
import com.institucion.rutas.dominio.Sede;
import com.institucion.rutas.dominio.SedeInexistenteException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Camino de costo mínimo con el algoritmo de Dijkstra.
 *
 * <h2>Complejidad</h2>
 * <ul>
 *   <li><b>Tiempo:</b> O((V + E) · log V). Cada arista puede insertar una entrada
 *       en la {@link PriorityQueue} (O(log V) amortizado; la cola puede llegar a E
 *       entradas porque se usa "eliminación perezosa" en lugar de decrease-key, y
 *       log E ≤ 2·log V). Cada sede se procesa una sola vez.</li>
 *   <li><b>Espacio:</b> O(V + E) por la instantánea del grafo, la cola y los
 *       mapas de distancias y predecesores.</li>
 *   <li>Reconstrucción de la ruta: O(longitud de la ruta) ≤ O(V).</li>
 * </ul>
 *
 * <p>Las distancias se acumulan en {@code long}: como cada costo cabe en un
 * {@code int} y una ruta simple tiene a lo sumo V-1 aristas, no hay desbordamiento
 * para ningún mapa que quepa en memoria.</p>
 *
 * <p>La clase no tiene estado y es segura para uso concurrente: trabaja sobre una
 * instantánea inmutable del mapa.</p>
 */
public final class CalculadorRutasDijkstra implements CalculadorRutas {

    private record Entrada(String sede, long distancia) {
    }

    @Override
    public ResultadoRuta calcular(MapaSedes mapa, String origen, String destino) {
        Objects.requireNonNull(mapa, "mapa");
        Map<String, List<Conexion>> grafo = mapa.adyacencia();
        String o = requerirSede(grafo, origen);
        String d = requerirSede(grafo, destino);

        if (o.equals(d)) {
            return ResultadoRuta.encontrada(o, d, List.of(o), 0L);
        }

        Map<String, Long> distancia = new HashMap<>();
        Map<String, String> previo = new HashMap<>();
        Set<String> procesadas = new HashSet<>();
        PriorityQueue<Entrada> cola = new PriorityQueue<>(Comparator.comparingLong(Entrada::distancia));

        distancia.put(o, 0L);
        cola.add(new Entrada(o, 0L));

        while (!cola.isEmpty()) {
            Entrada actual = cola.poll();
            // Entrada obsoleta: la sede ya se procesó con una distancia menor.
            if (!procesadas.add(actual.sede())) {
                continue;
            }
            if (actual.sede().equals(d)) {
                break;
            }
            for (Conexion conexion : grafo.get(actual.sede())) {
                String vecino = conexion.destino();
                if (procesadas.contains(vecino)) {
                    continue;
                }
                long candidata = actual.distancia() + conexion.costo();
                Long conocida = distancia.get(vecino);
                if (conocida == null || candidata < conocida) {
                    distancia.put(vecino, candidata);
                    previo.put(vecino, actual.sede());
                    cola.add(new Entrada(vecino, candidata));
                }
            }
        }

        if (!procesadas.contains(d)) {
            return ResultadoRuta.sinRuta(o, d);
        }
        return ResultadoRuta.encontrada(o, d, reconstruir(previo, o, d), distancia.get(d));
    }

    private static List<String> reconstruir(Map<String, String> previo, String origen, String destino) {
        List<String> ruta = new ArrayList<>();
        for (String sede = destino; sede != null; sede = previo.get(sede)) {
            ruta.add(sede);
            if (sede.equals(origen)) {
                break;
            }
        }
        Collections.reverse(ruta);
        return ruta;
    }

    private static String requerirSede(Map<String, List<Conexion>> grafo, String id) {
        String clave = Sede.validarId(id);
        if (!grafo.containsKey(clave)) {
            throw new SedeInexistenteException(clave);
        }
        return clave;
    }
}
