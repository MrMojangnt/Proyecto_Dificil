package com.institucion.rutas.persistencia;

import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.ResultadoRuta;
import com.institucion.rutas.dominio.Sede;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatoMapaTest {

    @TempDir
    Path dir;

    @Test
    void serializarYParsearConservaSedesYConexiones() {
        MapaSedes mapa = new MapaSedes();
        mapa.agregarSede(new Sede("CENTRAL", "Sede Central Ficticia"));
        mapa.agregarSede(new Sede("NORTE", "Campus Norte"));
        mapa.agregarConexion("CENTRAL", "NORTE", 12);

        MapaSedes leido = FormatoMapa.parsear(FormatoMapa.serializar(mapa).lines().toList(), "prueba");

        assertEquals(mapa.sedes(), leido.sedes());
        assertEquals(mapa.conexiones(), leido.conexiones());
    }

    @Test
    void archivoInexistenteEnRepositorioDevuelveMapaVacio() {
        MapaSedes mapa = new ArchivoMapaRepository(dir.resolve("no-existe.txt")).cargar();
        assertEquals(0, mapa.cantidadSedes());
    }

    @Test
    void importarArchivoInexistenteDaErrorControlado() {
        PersistenciaException e = assertThrows(PersistenciaException.class,
                () -> FormatoMapa.leer(dir.resolve("falta.txt")));
        assertTrue(e.getMessage().contains("no existe"));
    }

    @Test
    void erroresDeFormatoIndicanLaLinea() {
        assertLinea(2, "SEDE;A;Sede A\nCONEXION;A;Z;3");          // sede inexistente
        assertLinea(1, "CONEXION;A;B;tres");                     // costo no numérico
        assertLinea(1, "CONEXION;A;B;9999999999");               // fuera de rango int
        assertLinea(3, "# comentario\n\nRUTA;A;B");              // tipo desconocido
        assertLinea(2, "SEDE;A;Sede A\nSEDE;A;Repetida");        // duplicada
        assertLinea(1, "SEDE;A");                                // campos insuficientes
    }

    @Test
    void historialCorruptoDaErrorControlado() throws Exception {
        Path archivo = dir.resolve("historial.txt");
        ArchivoHistorialRepository repo = new ArchivoHistorialRepository(archivo);
        repo.registrar(new RegistroCalculo(Instant.parse("2026-01-01T00:00:00Z"), ResultadoRuta.sinRuta("A", "B")));
        Files.writeString(archivo, "basura;sin;formato\n", java.nio.file.StandardOpenOption.APPEND);

        FormatoArchivoException e = assertThrows(FormatoArchivoException.class, repo::cargar);
        assertEquals(3, e.getLinea());
    }

    private static void assertLinea(int linea, String contenido) {
        FormatoArchivoException e = assertThrows(FormatoArchivoException.class,
                () -> FormatoMapa.parsear(List.of(contenido.split("\n", -1)), "entrada"));
        assertEquals(linea, e.getLinea(), e.getMessage());
    }
}
