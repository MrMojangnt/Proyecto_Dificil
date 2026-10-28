package com.institucion.rutas.persistencia;

import com.institucion.rutas.dominio.EstadoRuta;
import com.institucion.rutas.dominio.ResultadoRuta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Historial de cálculos en un archivo de texto, un registro por línea:
 * {@code fecha;origen;destino;estado;costo;ruta} donde la ruta usa '&gt;' como
 * separador (p. ej. {@code A>C>B}). Sólo se agregan líneas al final.
 */
public final class ArchivoHistorialRepository implements HistorialRepository {

    static final String CABECERA = "# fecha;origen;destino;estado;costo;ruta";

    private final Path archivo;

    public ArchivoHistorialRepository(Path archivo) {
        this.archivo = Objects.requireNonNull(archivo);
    }

    @Override
    public synchronized void registrar(RegistroCalculo registro) {
        ResultadoRuta r = registro.resultado();
        String linea = String.join(";", registro.fecha().toString(), r.origen(), r.destino(),
                r.estado().name(), Long.toString(r.costoTotal()), String.join(">", r.ruta())) + "\n";
        try {
            Path absoluto = archivo.toAbsolutePath();
            Files.createDirectories(absoluto.getParent());
            if (!Files.exists(absoluto)) {
                linea = CABECERA + "\n" + linea;
            }
            Files.writeString(absoluto, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo registrar el cálculo en " + archivo + ": " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized List<RegistroCalculo> cargar() {
        if (!Files.exists(archivo)) {
            return List.of();
        }
        List<String> lineas;
        try {
            lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el historial " + archivo + ": " + e.getMessage(), e);
        }
        String nombre = archivo.getFileName().toString();
        List<RegistroCalculo> registros = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).strip();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            registros.add(parsear(linea, nombre, i + 1));
        }
        return List.copyOf(registros);
    }

    private static RegistroCalculo parsear(String linea, String nombre, int numero) {
        String[] c = linea.split(";", -1);
        if (c.length != 6) {
            throw new FormatoArchivoException(nombre, numero, "se esperaban 6 campos: " + CABECERA.substring(2));
        }
        try {
            Instant fecha = Instant.parse(c[0]);
            EstadoRuta estado = EstadoRuta.valueOf(c[3]);
            long costo = Long.parseLong(c[4]);
            List<String> ruta = c[5].isEmpty() ? List.of() : Arrays.asList(c[5].split(">"));
            return new RegistroCalculo(fecha, new ResultadoRuta(c[1], c[2], estado, ruta, costo));
        } catch (RuntimeException e) {
            throw new FormatoArchivoException(nombre, numero, "registro inválido (" + e.getMessage() + ")");
        }
    }
}
