package org.institucion.rutas;

import org.institucion.rutas.domain.DirectedMap;
import org.institucion.rutas.persistence.MapFileRepository;
import org.institucion.rutas.service.RouteResult;
import org.institucion.rutas.service.RouteService;
import org.institucion.rutas.service.RouteStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class Main {
    private static final Path DEFAULT_FILE = Path.of("data", "map.tsv");

    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException | IOException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(2);
        }
    }

    private static void run(String[] args) throws IOException {
        if (args.length == 0 || "help".equals(args[0])) {
            printHelp();
            return;
        }
        MapFileRepository repository = new MapFileRepository();
        Path storage = Path.of(System.getenv().getOrDefault("MAP_FILE", DEFAULT_FILE.toString()));
        DirectedMap map = repository.load(storage);
        switch (args[0]) {
            case "add-site" -> {
                requireCount(args, 2, "add-site <sede>");
                map.addSite(args[1]);
                repository.save(storage, map);
                System.out.println("Sede agregada: " + args[1].trim());
            }
            case "add-connection" -> {
                requireCount(args, 4, "add-connection <origen> <destino> <costo-entero>");
                int cost;
                try {
                    cost = Integer.parseInt(args[3]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("El costo debe ser un entero.");
                }
                map.addConnection(args[1], args[2], cost);
                repository.save(storage, map);
                System.out.println("Conexión agregada: " + args[1] + " -> " + args[2] + " (" + cost + ")");
            }
            case "route" -> {
                requireCount(args, 3, "route <origen> <destino>");
                RouteResult result = new RouteService(map).shortestRoute(args[1], args[2]);
                if (result.status() == RouteStatus.SIN_RUTA) {
                    System.out.println("SIN_RUTA");
                } else {
                    System.out.println("Ruta: " + String.join(" -> ", result.route()));
                    System.out.println("Costo total: " + result.totalCost());
                }
            }
            case "list" -> {
                requireCount(args, 1, "list");
                System.out.println("Sedes: " + map.sites().stream().map(site -> site.name())
                        .collect(Collectors.joining(", ")));
                map.connections().forEach(edge -> System.out.println(edge.origin() + " -> "
                        + edge.destination() + " (" + edge.cost() + ")"));
            }
            case "import" -> {
                requireCount(args, 2, "import <archivo>");
                DirectedMap imported = repository.load(Path.of(args[1]));
                repository.save(storage, imported);
                System.out.println("Mapa importado desde " + args[1]);
            }
            case "export" -> {
                requireCount(args, 2, "export <archivo>");
                repository.save(Path.of(args[1]), map);
                System.out.println("Mapa exportado a " + args[1]);
            }
            default -> throw new IllegalArgumentException("Comando desconocido: " + args[0]
                    + ". Use 'help' para ver los comandos.");
        }
    }

    private static void requireCount(String[] args, int expected, String usage) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Uso: " + usage);
        }
    }

    private static void printHelp() {
        System.out.println("""
                Uso: java -jar target/rutas-sedes-1.0.0.jar <comando>
                  add-site <sede>
                  add-connection <origen> <destino> <costo-entero-no-negativo>
                  route <origen> <destino>
                  list
                  import <archivo>
                  export <archivo>
                El archivo de negocio predeterminado es data/map.tsv; configure MAP_FILE para cambiarlo.
                """);
    }
}
