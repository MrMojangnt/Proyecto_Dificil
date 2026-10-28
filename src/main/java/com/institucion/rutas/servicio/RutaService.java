package com.institucion.rutas.servicio;

import com.institucion.rutas.dominio.Conexion;
import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.ResultadoRuta;
import com.institucion.rutas.dominio.Sede;
import com.institucion.rutas.persistencia.FormatoMapa;
import com.institucion.rutas.persistencia.HistorialRepository;
import com.institucion.rutas.persistencia.MapaRepository;
import com.institucion.rutas.persistencia.RegistroCalculo;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

/**
 * Casos de uso de la aplicación. Coordina el dominio y la persistencia.
 *
 * <p>Cada modificación se aplica sobre una copia del mapa que sólo reemplaza al
 * mapa vigente después de guardarse con éxito: si la validación o la escritura
 * fallan, el estado en memoria y en disco permanece intacto.</p>
 */
public final class RutaService {

    private final MapaRepository mapaRepository;
    private final HistorialRepository historialRepository;
    private final CalculadorRutas calculador;
    private final Clock reloj;
    private volatile MapaSedes mapa;

    public RutaService(MapaRepository mapaRepository, HistorialRepository historialRepository) {
        this(mapaRepository, historialRepository, new CalculadorRutasDijkstra(), Clock.systemUTC());
    }

    public RutaService(MapaRepository mapaRepository, HistorialRepository historialRepository,
                       CalculadorRutas calculador, Clock reloj) {
        this.mapaRepository = Objects.requireNonNull(mapaRepository);
        this.historialRepository = Objects.requireNonNull(historialRepository);
        this.calculador = Objects.requireNonNull(calculador);
        this.reloj = Objects.requireNonNull(reloj);
        this.mapa = mapaRepository.cargar();
    }

    public synchronized void agregarSede(String id, String nombre) {
        MapaSedes nuevo = mapa.copia();
        nuevo.agregarSede(new Sede(id, nombre));
        confirmar(nuevo);
    }

    public synchronized void eliminarSede(String id) {
        MapaSedes nuevo = mapa.copia();
        nuevo.eliminarSede(id);
        confirmar(nuevo);
    }

    public synchronized void agregarConexion(String origen, String destino, int costo) {
        Conexion conexion = new Conexion(origen, destino, costo); // valida el costo antes de copiar
        MapaSedes nuevo = mapa.copia();
        nuevo.agregarConexion(conexion);
        confirmar(nuevo);
    }

    public synchronized boolean eliminarConexion(String origen, String destino) {
        MapaSedes nuevo = mapa.copia();
        boolean eliminada = nuevo.eliminarConexion(origen, destino);
        if (eliminada) {
            confirmar(nuevo);
        }
        return eliminada;
    }

    /** Calcula el trayecto y lo registra en el historial persistente. */
    public ResultadoRuta calcularRuta(String origen, String destino) {
        ResultadoRuta resultado = calculador.calcular(mapa, origen, destino);
        historialRepository.registrar(new RegistroCalculo(reloj.instant(), resultado));
        return resultado;
    }

    /**
     * Reemplaza el mapa actual por el contenido del archivo. El archivo se valida
     * completo antes de aplicar cualquier cambio.
     */
    public synchronized void importar(Path archivo) {
        MapaSedes importado = FormatoMapa.leer(archivo);
        confirmar(importado);
    }

    public void exportar(Path archivo) {
        FormatoMapa.escribir(mapa, archivo);
    }

    public MapaSedes mapa() {
        return mapa.copia();
    }

    public List<RegistroCalculo> historial() {
        return historialRepository.cargar();
    }

    private void confirmar(MapaSedes nuevo) {
        mapaRepository.guardar(nuevo);
        mapa = nuevo;
    }
}
