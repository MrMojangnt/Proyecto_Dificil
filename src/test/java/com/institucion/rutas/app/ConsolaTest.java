package com.institucion.rutas.app;

import com.institucion.rutas.persistencia.ArchivoHistorialRepository;
import com.institucion.rutas.persistencia.ArchivoMapaRepository;
import com.institucion.rutas.servicio.RutaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsolaTest {

    @TempDir
    Path dir;

    private Consola consola;

    @BeforeEach
    void preparar() {
        consola = new Consola(new RutaService(new ArchivoMapaRepository(dir.resolve("mapa.txt")),
                new ArchivoHistorialRepository(dir.resolve("historial.txt"))));
    }

    @Test
    void flujoCompletoPorComandos() {
        consola.ejecutar("sede A Sede Administrativa");
        consola.ejecutar("sede B");
        consola.ejecutar("sede C");
        consola.ejecutar("conexion A B 4");
        consola.ejecutar("conexion A C 1");
        consola.ejecutar("conexion C B 2");

        String salida = consola.ejecutar("ruta A B");
        assertTrue(salida.contains("A -> C -> B"), salida);
        assertTrue(salida.contains("Costo total: 3"), salida);
    }

    @Test
    void entradasInvalidasDevuelvenMensajesControlados() {
        consola.ejecutar("sede A");
        consola.ejecutar("sede B");
        assertTrue(consola.ejecutar("conexion A B -1").startsWith("ERROR: Costo inválido -1"));
        assertTrue(consola.ejecutar("conexion A B x").startsWith("ERROR:"));
        assertTrue(consola.ejecutar("conexion A B").startsWith("ERROR: uso:"));
        assertTrue(consola.ejecutar("ruta A Z").contains("no existe"));
        assertTrue(consola.ejecutar("importar " + dir.resolve("nada.txt")).startsWith("ERROR:"));
        assertTrue(consola.ejecutar("volar").startsWith("ERROR: comando desconocido"));
        assertTrue(consola.ejecutar("ruta A B").startsWith("SIN_RUTA"));
        assertEquals("", consola.ejecutar("   "));
    }
}
