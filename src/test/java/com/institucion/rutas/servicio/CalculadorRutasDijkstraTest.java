package com.institucion.rutas.servicio;

import com.institucion.rutas.dominio.EstadoRuta;
import com.institucion.rutas.dominio.MapaException;
import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.ResultadoRuta;
import com.institucion.rutas.dominio.Sede;
import com.institucion.rutas.dominio.SedeInexistenteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadorRutasDijkstraTest {

    private final CalculadorRutas calculador = new CalculadorRutasDijkstra();

    private static MapaSedes mapaCon(String... sedes) {
        MapaSedes mapa = new MapaSedes();
        for (String s : sedes) {
            mapa.agregarSede(new Sede(s));
        }
        return mapa;
    }

    @Test
    @DisplayName("Criterio 1: A→B=4, A→C=1, C→B=2 da costo 3 y ruta A,C,B")
    void criterioRutaIndirectaMasBarata() {
        MapaSedes mapa = mapaCon("A", "B", "C");
        mapa.agregarConexion("A", "B", 4);
        mapa.agregarConexion("A", "C", 1);
        mapa.agregarConexion("C", "B", 2);

        ResultadoRuta r = calculador.calcular(mapa, "A", "B");

        assertEquals(EstadoRuta.ENCONTRADA, r.estado());
        assertEquals(3L, r.costoTotal());
        assertEquals(List.of("A", "C", "B"), r.ruta());
    }

    @Test
    @DisplayName("Criterio 2: una sede aislada produce SIN_RUTA sin excepción")
    void sedeAisladaDevuelveSinRuta() {
        MapaSedes mapa = mapaCon("A", "B", "AISLADA");
        mapa.agregarConexion("A", "B", 1);

        ResultadoRuta haciaAislada = assertDoesNotThrow(() -> calculador.calcular(mapa, "A", "AISLADA"));
        ResultadoRuta desdeAislada = assertDoesNotThrow(() -> calculador.calcular(mapa, "AISLADA", "A"));

        assertEquals(EstadoRuta.SIN_RUTA, haciaAislada.estado());
        assertEquals(EstadoRuta.SIN_RUTA, desdeAislada.estado());
        assertTrue(haciaAislada.ruta().isEmpty());
        assertEquals(ResultadoRuta.SIN_COSTO, haciaAislada.costoTotal());
    }

    @Test
    void origenIgualADestinoCuestaCero() {
        MapaSedes mapa = mapaCon("A");
        ResultadoRuta r = calculador.calcular(mapa, "A", "A");
        assertEquals(EstadoRuta.ENCONTRADA, r.estado());
        assertEquals(0L, r.costoTotal());
        assertEquals(List.of("A"), r.ruta());
    }

    @Test
    void sedeInexistenteSeRechazaComoOrigenYDestino() {
        MapaSedes mapa = mapaCon("A");
        assertThrows(SedeInexistenteException.class, () -> calculador.calcular(mapa, "X", "A"));
        assertThrows(SedeInexistenteException.class, () -> calculador.calcular(mapa, "A", "X"));
        assertThrows(SedeInexistenteException.class, () -> calculador.calcular(mapa, "X", "X"));
        assertThrows(MapaException.class, () -> calculador.calcular(mapa, null, "A"));
    }

    @Test
    void respetaLaDireccionDeLasConexiones() {
        MapaSedes mapa = mapaCon("A", "B");
        mapa.agregarConexion("A", "B", 7);
        assertEquals(EstadoRuta.SIN_RUTA, calculador.calcular(mapa, "B", "A").estado());
    }

    @Test
    void conexionesDeCostoCero() {
        MapaSedes mapa = mapaCon("A", "B", "C");
        mapa.agregarConexion("A", "B", 0);
        mapa.agregarConexion("B", "C", 0);
        mapa.agregarConexion("A", "C", 1);
        ResultadoRuta r = calculador.calcular(mapa, "A", "C");
        assertEquals(0L, r.costoTotal());
        assertEquals(List.of("A", "B", "C"), r.ruta());
    }

    @Test
    void costosGrandesSeAcumulanEnLongSinDesbordar() {
        MapaSedes mapa = mapaCon("A", "B", "C", "D");
        mapa.agregarConexion("A", "B", Integer.MAX_VALUE);
        mapa.agregarConexion("B", "C", Integer.MAX_VALUE);
        mapa.agregarConexion("C", "D", Integer.MAX_VALUE);
        ResultadoRuta r = calculador.calcular(mapa, "A", "D");
        assertEquals(3L * Integer.MAX_VALUE, r.costoTotal());
    }

    @Test
    void ignoraCiclosYEligeElMinimoEnGrafoMayor() {
        // Grafo clásico con ciclo B<->C; mínimo A->C->B->D = 2+1+1 = 4 (vs A->B->D = 5)
        MapaSedes mapa = mapaCon("A", "B", "C", "D", "E");
        mapa.agregarConexion("A", "B", 4);
        mapa.agregarConexion("A", "C", 2);
        mapa.agregarConexion("C", "B", 1);
        mapa.agregarConexion("B", "C", 1);
        mapa.agregarConexion("B", "D", 1);
        mapa.agregarConexion("C", "D", 5);
        mapa.agregarConexion("D", "E", 3);
        mapa.agregarConexion("E", "A", 1);

        ResultadoRuta r = calculador.calcular(mapa, "A", "E");
        assertEquals(7L, r.costoTotal());
        assertEquals(List.of("A", "C", "B", "D", "E"), r.ruta());
    }

    @Test
    void coincideConFuerzaBrutaEnGrafosAleatorios() {
        Random azar = new Random(42);
        for (int prueba = 0; prueba < 50; prueba++) {
            int n = 2 + azar.nextInt(8);
            MapaSedes mapa = new MapaSedes();
            long[][] dist = new long[n][n];
            for (int i = 0; i < n; i++) {
                mapa.agregarSede(new Sede("S" + i));
                for (int j = 0; j < n; j++) {
                    dist[i][j] = i == j ? 0 : Long.MAX_VALUE / 4;
                }
            }
            for (int k = 0; k < n * 2; k++) {
                int o = azar.nextInt(n);
                int d = azar.nextInt(n);
                int costo = azar.nextInt(20);
                mapa.agregarConexion("S" + o, "S" + d, costo); // reemplaza si ya existía
                if (o != d) {
                    dist[o][d] = mapa.costoConexion("S" + o, "S" + d).orElseThrow();
                }
            }
            // Floyd-Warshall como referencia independiente
            for (int k = 0; k < n; k++) {
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        dist[i][j] = Math.min(dist[i][j], dist[i][k] + dist[k][j]);
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    ResultadoRuta r = calculador.calcular(mapa, "S" + i, "S" + j);
                    if (dist[i][j] >= Long.MAX_VALUE / 4) {
                        assertEquals(EstadoRuta.SIN_RUTA, r.estado());
                    } else {
                        assertEquals(dist[i][j], r.costoTotal());
                        assertEquals(r.costoTotal(), costoDeRuta(mapa, r.ruta()), "la ruta debe ser válida");
                    }
                }
            }
        }
    }

    @Test
    void calculosConcurrentesSobreElMismoMapa() throws Exception {
        MapaSedes mapa = mapaCon("A", "B", "C");
        mapa.agregarConexion("A", "B", 4);
        mapa.agregarConexion("A", "C", 1);
        mapa.agregarConexion("C", "B", 2);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<ResultadoRuta>> tareas = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                tareas.add(() -> calculador.calcular(mapa, "A", "B"));
            }
            for (Future<ResultadoRuta> f : pool.invokeAll(tareas)) {
                assertEquals(3L, f.get().costoTotal());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static long costoDeRuta(MapaSedes mapa, List<String> ruta) {
        long total = 0;
        for (int i = 0; i + 1 < ruta.size(); i++) {
            total += mapa.costoConexion(ruta.get(i), ruta.get(i + 1)).orElseThrow();
        }
        return total;
    }
}
