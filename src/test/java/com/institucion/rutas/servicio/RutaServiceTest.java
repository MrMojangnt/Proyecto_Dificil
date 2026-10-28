package com.institucion.rutas.servicio;

import com.institucion.rutas.dominio.EstadoRuta;
import com.institucion.rutas.dominio.PesoInvalidoException;
import com.institucion.rutas.dominio.ResultadoRuta;
import com.institucion.rutas.persistencia.ArchivoHistorialRepository;
import com.institucion.rutas.persistencia.ArchivoMapaRepository;
import com.institucion.rutas.persistencia.FormatoArchivoException;
import com.institucion.rutas.persistencia.RegistroCalculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RutaServiceTest {

    @TempDir
    Path dir;

    private RutaService nuevoServicio() {
        return new RutaService(new ArchivoMapaRepository(dir.resolve("mapa.txt")),
                new ArchivoHistorialRepository(dir.resolve("historial.txt")));
    }

    private static void cargarEjemplo(RutaService s) {
        s.agregarSede("A", "Sede A");
        s.agregarSede("B", "Sede B");
        s.agregarSede("C", "Sede C");
        s.agregarConexion("A", "B", 4);
        s.agregarConexion("A", "C", 1);
        s.agregarConexion("C", "B", 2);
    }

    @Test
    void elEstadoYElHistorialSobrevivenAUnReinicio() {
        RutaService primero = nuevoServicio();
        cargarEjemplo(primero);
        primero.calcularRuta("A", "B");
        primero.calcularRuta("B", "A");

        RutaService reiniciado = nuevoServicio();

        assertEquals(3, reiniciado.mapa().cantidadSedes());
        assertEquals(3, reiniciado.mapa().cantidadConexiones());
        assertEquals(List.of("A", "C", "B"), reiniciado.calcularRuta("A", "B").ruta());

        List<RegistroCalculo> historial = reiniciado.historial();
        assertEquals(3, historial.size());
        assertEquals(List.of("A", "C", "B"), historial.get(0).resultado().ruta());
        assertEquals(3L, historial.get(0).resultado().costoTotal());
        assertEquals(EstadoRuta.SIN_RUTA, historial.get(1).resultado().estado());
    }

    @Test
    void pesoNegativoNoSeAgregaNiSePersiste() throws Exception {
        RutaService s = nuevoServicio();
        cargarEjemplo(s);
        String antes = Files.readString(dir.resolve("mapa.txt"));

        assertThrows(PesoInvalidoException.class, () -> s.agregarConexion("B", "C", -1));

        assertEquals(3, s.mapa().cantidadConexiones());
        assertEquals(antes, Files.readString(dir.resolve("mapa.txt")));
        assertEquals(3, nuevoServicio().mapa().cantidadConexiones());
    }

    @Test
    void exportarEImportarReproduceElMismoMapa() {
        RutaService s = nuevoServicio();
        cargarEjemplo(s);
        Path exportado = dir.resolve("exportado.txt");
        s.exportar(exportado);

        s.eliminarSede("C");
        assertEquals(EstadoRuta.ENCONTRADA, s.calcularRuta("A", "B").estado());
        assertEquals(4L, s.calcularRuta("A", "B").costoTotal());

        s.importar(exportado);
        ResultadoRuta r = s.calcularRuta("A", "B");
        assertEquals(3L, r.costoTotal());
        assertEquals(3, nuevoServicio().mapa().cantidadSedes(), "la importación debe persistirse");
    }

    @Test
    void importacionInvalidaNoModificaElMapa() throws Exception {
        RutaService s = nuevoServicio();
        cargarEjemplo(s);
        Path malo = dir.resolve("malo.txt");
        Files.writeString(malo, """
                SEDE;X;Sede X
                SEDE;Y;Sede Y
                CONEXION;X;Y;-1
                """);

        FormatoArchivoException e = assertThrows(FormatoArchivoException.class, () -> s.importar(malo));
        assertEquals(3, e.getLinea());
        assertTrue(e.getMessage().contains("-1"));
        assertTrue(s.mapa().contieneSede("A"));
        assertEquals(3, s.mapa().cantidadSedes());
    }
}
