package com.institucion.rutas.app;

import com.institucion.rutas.persistencia.ArchivoHistorialRepository;
import com.institucion.rutas.persistencia.ArchivoMapaRepository;
import com.institucion.rutas.persistencia.PersistenciaException;
import com.institucion.rutas.servicio.RutaService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Punto de entrada de consola.
 *
 * <pre>
 * java -jar rutas-sedes.jar [--datos DIR]                 modo interactivo (o comandos por stdin)
 * java -jar rutas-sedes.jar [--datos DIR] ruta A B        ejecuta un único comando
 * </pre>
 *
 * El estado se guarda en {@code DIR/mapa.txt} y {@code DIR/historial.txt}
 * (por defecto {@code DIR = ./datos-app}).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Path datos = Path.of("datos-app");
        int inicio = 0;
        if (args.length >= 2 && args[0].equals("--datos")) {
            datos = Path.of(args[1]);
            inicio = 2;
        }

        RutaService servicio;
        try {
            servicio = new RutaService(
                    new ArchivoMapaRepository(datos.resolve("mapa.txt")),
                    new ArchivoHistorialRepository(datos.resolve("historial.txt")));
        } catch (PersistenciaException e) {
            System.err.println("ERROR: no se pudo cargar el estado guardado. " + e.getMessage());
            System.exit(1);
            return;
        }
        Consola consola = new Consola(servicio);

        if (inicio < args.length) {
            String salida = consola.ejecutar(String.join(" ", Arrays.copyOfRange(args, inicio, args.length)));
            System.out.println(salida);
            System.exit(salida.startsWith("ERROR:") ? 2 : 0);
            return;
        }

        boolean interactivo = System.console() != null;
        if (interactivo) {
            System.out.println("Rutas entre sedes. Datos en " + datos.toAbsolutePath() + ". Escriba 'ayuda'.");
        }
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            if (interactivo) {
                System.out.print("> ");
                System.out.flush();
            }
            String linea = entrada.readLine();
            if (linea == null || linea.strip().equalsIgnoreCase("salir")) {
                break;
            }
            String salida = consola.ejecutar(linea);
            if (!salida.isEmpty()) {
                System.out.println(salida);
            }
        }
    }
}
