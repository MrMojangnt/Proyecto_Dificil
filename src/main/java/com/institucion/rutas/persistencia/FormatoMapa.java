package com.institucion.rutas.persistencia;

import com.institucion.rutas.dominio.Conexion;
import com.institucion.rutas.dominio.MapaException;
import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.Sede;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/**
 * Formato de texto del mapa, usado tanto para el estado persistido como para
 * importar y exportar. Una entrada por línea, campos separados por ';':
 *
 * <pre>
 * # comentario
 * SEDE;&lt;id&gt;;&lt;nombre&gt;
 * CONEXION;&lt;origen&gt;;&lt;destino&gt;;&lt;costo&gt;
 * </pre>
 *
 * Las sedes deben declararse antes de las conexiones que las usan. Las líneas
 * vacías y las que empiezan por '#' se ignoran.
 */
public final class FormatoMapa {

    public static final String CABECERA = "# Mapa de sedes v1 (SEDE;id;nombre | CONEXION;origen;destino;costo)";

    private FormatoMapa() {
    }

    public static String serializar(MapaSedes mapa) {
        StringBuilder sb = new StringBuilder(CABECERA).append('\n');
        for (Sede sede : mapa.sedes()) {
            sb.append("SEDE;").append(sede.id()).append(';').append(sede.nombre()).append('\n');
        }
        for (Conexion c : mapa.conexiones()) {
            sb.append("CONEXION;").append(c.origen()).append(';').append(c.destino())
                    .append(';').append(c.costo()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Construye un mapa a partir de líneas de texto. Valida todo el contenido:
     * ante el primer error lanza una excepción y no devuelve un mapa parcial.
     *
     * @param nombreOrigen nombre del archivo, sólo para los mensajes de error
     */
    public static MapaSedes parsear(List<String> lineas, String nombreOrigen) {
        MapaSedes mapa = new MapaSedes();
        for (int i = 0; i < lineas.size(); i++) {
            int numero = i + 1;
            String linea = lineas.get(i).strip();
            if (numero == 1 && linea.startsWith("﻿")) {
                linea = linea.substring(1);
            }
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            String[] campos = linea.split(";", -1);
            try {
                switch (campos[0].strip().toUpperCase()) {
                    case "SEDE" -> {
                        exigirCampos(campos, 3, "SEDE;id;nombre", nombreOrigen, numero);
                        mapa.agregarSede(new Sede(campos[1], campos[2]));
                    }
                    case "CONEXION" -> {
                        exigirCampos(campos, 4, "CONEXION;origen;destino;costo", nombreOrigen, numero);
                        int costo = parsearCosto(campos[3], nombreOrigen, numero);
                        mapa.agregarConexion(new Conexion(campos[1], campos[2], costo));
                    }
                    default -> throw new FormatoArchivoException(nombreOrigen, numero,
                            "tipo de registro desconocido '" + campos[0] + "' (se espera SEDE o CONEXION)");
                }
            } catch (MapaException e) {
                throw new FormatoArchivoException(nombreOrigen, numero, e.getMessage());
            }
        }
        return mapa;
    }

    public static MapaSedes leer(Path archivo) {
        try {
            return parsear(Files.readAllLines(archivo, StandardCharsets.UTF_8), archivo.getFileName().toString());
        } catch (NoSuchFileException e) {
            throw new PersistenciaException("El archivo " + archivo + " no existe", e);
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el archivo " + archivo + ": " + e.getMessage(), e);
        }
    }

    public static void escribir(MapaSedes mapa, Path archivo) {
        Archivos.escribirAtomico(archivo, serializar(mapa));
    }

    private static void exigirCampos(String[] campos, int esperados, String formato, String origen, int linea) {
        if (campos.length != esperados) {
            throw new FormatoArchivoException(origen, linea,
                    "se esperaban " + esperados + " campos con el formato " + formato);
        }
    }

    private static int parsearCosto(String texto, String origen, int linea) {
        long costo;
        try {
            costo = Long.parseLong(texto.strip());
        } catch (NumberFormatException e) {
            throw new FormatoArchivoException(origen, linea, "costo '" + texto + "' no es un número entero");
        }
        if (costo > Integer.MAX_VALUE) {
            throw new FormatoArchivoException(origen, linea, "costo " + costo + " excede el máximo " + Integer.MAX_VALUE);
        }
        if (costo < 0) {
            throw new FormatoArchivoException(origen, linea,
                    "costo inválido " + costo + ": el costo de una conexión debe ser un entero no negativo");
        }
        return (int) costo;
    }
}
