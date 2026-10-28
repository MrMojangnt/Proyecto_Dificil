package com.institucion.rutas.app;

import com.institucion.rutas.dominio.Conexion;
import com.institucion.rutas.dominio.MapaException;
import com.institucion.rutas.dominio.MapaSedes;
import com.institucion.rutas.dominio.ResultadoRuta;
import com.institucion.rutas.dominio.Sede;
import com.institucion.rutas.persistencia.PersistenciaException;
import com.institucion.rutas.persistencia.RegistroCalculo;
import com.institucion.rutas.servicio.RutaService;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Intérprete de comandos de texto. Cada llamada a {@link #ejecutar(String)}
 * procesa una línea y devuelve la respuesta; los errores se devuelven como
 * mensajes con prefijo {@code ERROR:} en lugar de propagarse.
 */
public final class Consola {

    static final String AYUDA = """
            Comandos disponibles:
              sede <ID> [nombre]                 Registra una sede
              conexion <ORIGEN> <DESTINO> <COSTO> Crea o reemplaza una conexión dirigida
              quitar-sede <ID>                   Elimina una sede y sus conexiones
              quitar-conexion <ORIGEN> <DESTINO> Elimina una conexión
              ruta <ORIGEN> <DESTINO>            Calcula el trayecto de menor costo
              mapa                               Muestra sedes y conexiones
              historial                          Muestra los cálculos realizados
              importar <archivo>                 Reemplaza el mapa con el contenido del archivo
              exportar <archivo>                 Guarda el mapa en el archivo indicado
              ayuda                              Muestra esta ayuda
              salir                              Termina la aplicación""";

    private final RutaService servicio;

    public Consola(RutaService servicio) {
        this.servicio = servicio;
    }

    public String ejecutar(String linea) {
        String texto = linea == null ? "" : linea.strip();
        if (texto.isEmpty() || texto.startsWith("#")) {
            return "";
        }
        String[] partes = texto.split("\\s+", 2);
        String comando = partes[0].toLowerCase(Locale.ROOT);
        String resto = partes.length > 1 ? partes[1] : "";
        try {
            return switch (comando) {
                case "sede" -> sede(resto);
                case "conexion" -> conexion(resto);
                case "quitar-sede" -> quitarSede(resto);
                case "quitar-conexion" -> quitarConexion(resto);
                case "ruta" -> ruta(resto);
                case "mapa" -> mapa();
                case "historial" -> historial();
                case "importar" -> importar(resto);
                case "exportar" -> exportar(resto);
                case "ayuda", "help" -> AYUDA;
                default -> "ERROR: comando desconocido '" + partes[0] + "'. Escriba 'ayuda'.";
            };
        } catch (MapaException | PersistenciaException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String sede(String resto) {
        if (resto.isBlank()) {
            throw new MapaException("uso: sede <ID> [nombre]");
        }
        String[] args = resto.strip().split("\\s+", 2);
        servicio.agregarSede(args[0], args.length > 1 ? args[1] : null);
        return "Sede " + args[0] + " registrada.";
    }

    private String conexion(String resto) {
        String[] args = argumentos(resto, 3, "conexion <ORIGEN> <DESTINO> <COSTO>");
        int costo = entero(args[2]);
        servicio.agregarConexion(args[0], args[1], costo);
        return "Conexión " + args[0] + " -> " + args[1] + " (costo " + costo + ") registrada.";
    }

    private String quitarSede(String resto) {
        String[] args = argumentos(resto, 1, "quitar-sede <ID>");
        servicio.eliminarSede(args[0]);
        return "Sede " + args[0] + " eliminada.";
    }

    private String quitarConexion(String resto) {
        String[] args = argumentos(resto, 2, "quitar-conexion <ORIGEN> <DESTINO>");
        return servicio.eliminarConexion(args[0], args[1])
                ? "Conexión " + args[0] + " -> " + args[1] + " eliminada."
                : "No existe la conexión " + args[0] + " -> " + args[1] + ".";
    }

    private String ruta(String resto) {
        String[] args = argumentos(resto, 2, "ruta <ORIGEN> <DESTINO>");
        ResultadoRuta r = servicio.calcularRuta(args[0], args[1]);
        if (!r.tieneRuta()) {
            return "SIN_RUTA: no existe trayecto de " + r.origen() + " a " + r.destino() + ".";
        }
        return "Ruta: " + String.join(" -> ", r.ruta()) + System.lineSeparator()
                + "Costo total: " + r.costoTotal();
    }

    private String mapa() {
        MapaSedes mapa = servicio.mapa();
        StringBuilder sb = new StringBuilder();
        sb.append("Sedes (").append(mapa.cantidadSedes()).append("):");
        for (Sede s : mapa.sedes()) {
            sb.append(System.lineSeparator()).append("  ").append(s.id()).append(" - ").append(s.nombre());
        }
        sb.append(System.lineSeparator()).append("Conexiones (").append(mapa.cantidadConexiones()).append("):");
        for (Conexion c : mapa.conexiones()) {
            sb.append(System.lineSeparator()).append("  ").append(c.origen()).append(" -> ")
                    .append(c.destino()).append(" : ").append(c.costo());
        }
        return sb.toString();
    }

    private String historial() {
        List<RegistroCalculo> registros = servicio.historial();
        if (registros.isEmpty()) {
            return "Historial vacío.";
        }
        StringBuilder sb = new StringBuilder("Historial (").append(registros.size()).append("):");
        for (RegistroCalculo reg : registros) {
            ResultadoRuta r = reg.resultado();
            sb.append(System.lineSeparator()).append("  ").append(reg.fecha()).append("  ")
                    .append(r.origen()).append(" => ").append(r.destino()).append(": ")
                    .append(r.tieneRuta() ? String.join(" -> ", r.ruta()) + " (costo " + r.costoTotal() + ")"
                            : "SIN_RUTA");
        }
        return sb.toString();
    }

    private String importar(String resto) {
        Path archivo = ruta(resto, "importar <archivo>");
        servicio.importar(archivo);
        MapaSedes mapa = servicio.mapa();
        return "Mapa importado desde " + archivo + ": " + mapa.cantidadSedes() + " sedes, "
                + mapa.cantidadConexiones() + " conexiones.";
    }

    private String exportar(String resto) {
        Path archivo = ruta(resto, "exportar <archivo>");
        servicio.exportar(archivo);
        return "Mapa exportado a " + archivo + ".";
    }

    private static String[] argumentos(String resto, int cantidad, String uso) {
        String[] args = resto.isBlank() ? new String[0] : resto.strip().split("\\s+");
        if (args.length != cantidad) {
            throw new MapaException("uso: " + uso);
        }
        return args;
    }

    private static Path ruta(String resto, String uso) {
        String texto = resto.strip();
        if (texto.length() >= 2 && texto.startsWith("\"") && texto.endsWith("\"")) {
            texto = texto.substring(1, texto.length() - 1);
        }
        if (texto.isEmpty()) {
            throw new MapaException("uso: " + uso);
        }
        try {
            return Path.of(texto);
        } catch (RuntimeException e) {
            throw new MapaException("ruta de archivo inválida '" + texto + "'");
        }
    }

    private static int entero(String texto) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new MapaException("el costo '" + texto + "' debe ser un número entero entre 0 y "
                    + Integer.MAX_VALUE);
        }
    }
}
