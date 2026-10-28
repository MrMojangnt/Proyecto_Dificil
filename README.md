# Rutas entre sedes (Dijkstra)

Aplicación de consola en Java 17 que calcula el trayecto de menor costo sobre un
mapa **dirigido** de sedes. Implementa Dijkstra con `PriorityQueue` y distancias
`long`, persiste el mapa y el historial de cálculos en archivos de texto locales
y permite importar y exportar el mapa.

## Requisitos

- JDK 17 o superior (`java -version`).
- Maven 3.9.9: **no hace falta instalarlo**. El repositorio incluye Maven Wrapper
  (`./mvnw` en macOS/Linux, `mvnw.cmd` en Windows), que descarga esa versión exacta.
- Sin dependencias externas en tiempo de ejecución. Sólo JUnit 5.10.2 para pruebas.

## Compilar, probar y ejecutar

```bash
./mvnw test                 # ejecuta las 35 pruebas JUnit
./mvnw package              # compila, prueba y genera target/rutas-sedes-1.0.0.jar

# Modo interactivo (datos en ./datos-app)
java -jar target/rutas-sedes-1.0.0.jar

# Un único comando
java -jar target/rutas-sedes-1.0.0.jar --datos datos-demo ruta A B

# Demostración completa con guion
java -jar target/rutas-sedes-1.0.0.jar --datos datos-demo < datos/demo-comandos.txt
```

En Windows, use `mvnw.cmd` en lugar de `./mvnw`.

### Comandos

| Comando | Descripción |
|---|---|
| `sede <ID> [nombre]` | Registra una sede (ID: 1–40 letras, dígitos, `_` o `-`) |
| `conexion <ORIGEN> <DESTINO> <COSTO>` | Crea o reemplaza la conexión dirigida ORIGEN→DESTINO |
| `quitar-sede <ID>` | Elimina la sede y todas sus conexiones |
| `quitar-conexion <ORIGEN> <DESTINO>` | Elimina una conexión |
| `ruta <ORIGEN> <DESTINO>` | Calcula el trayecto de menor costo |
| `mapa` | Lista sedes y conexiones |
| `historial` | Lista los cálculos guardados |
| `importar <archivo>` | Reemplaza el mapa por el del archivo (se valida completo antes) |
| `exportar <archivo>` | Guarda el mapa actual en un archivo |
| `ayuda` / `salir` | Ayuda / terminar |

## Demostración (salida real)

```text
$ java -jar target/rutas-sedes-1.0.0.jar --datos datos-demo < datos/demo-comandos.txt
Mapa importado desde datos/ejemplo-mapa.txt: 7 sedes, 9 conexiones.
...
Ruta: A -> C -> B                       <- criterio 1
Costo total: 3
Ruta: A -> C -> B -> F -> E
Costo total: 10
SIN_RUTA: no existe trayecto de A a ISLA.   <- criterio 2
Ruta: B                                  <- origen = destino
Costo total: 0
ERROR: La sede 'NOEXISTE' no existe en el mapa
ERROR: Costo inválido -1: el costo de una conexión debe ser un entero no negativo   <- criterio 3
ERROR: ejemplo-invalido.txt, línea 5: costo inválido -1: el costo de una conexión debe ser un entero no negativo
Mapa exportado a datos-demo/exportado.txt.
Historial (4): ...

$ java -jar target/rutas-sedes-1.0.0.jar --datos datos-demo ruta A B     # tras reiniciar
Ruta: A -> C -> B
Costo total: 3
```

El segundo proceso recupera el mapa y el historial del disco sin volver a importar nada.

## Criterios de aceptación y dónde se prueban

| Criterio | Prueba |
|---|---|
| A→B=4, A→C=1, C→B=2 ⇒ costo 3, ruta A,C,B | `CalculadorRutasDijkstraTest.criterioRutaIndirectaMasBarata`, `ConsolaTest.flujoCompletoPorComandos` |
| Sede aislada ⇒ `SIN_RUTA` sin excepción | `CalculadorRutasDijkstraTest.sedeAisladaDevuelveSinRuta` |
| Peso -1 se rechaza sin añadir la conexión | `MapaSedesTest.pesoNegativoSeRechazaSinAgregar`, `RutaServiceTest.pesoNegativoNoSeAgregaNiSePersiste`, `RutaServiceTest.importacionInvalidaNoModificaElMapa` |
| Origen = destino ⇒ costo 0 | `CalculadorRutasDijkstraTest.origenIgualADestinoCuestaCero` |
| Sede inexistente rechazada | `CalculadorRutasDijkstraTest.sedeInexistenteSeRechazaComoOrigenYDestino`, `MapaSedesTest.conexionConSedeInexistenteSeRechaza` |
| Persistencia y recarga al reiniciar | `RutaServiceTest.elEstadoYElHistorialSobrevivenAUnReinicio` |
| Importar / exportar | `RutaServiceTest.exportarEImportarReproduceElMismoMapa`, `FormatoMapaTest.*` |

Otras pruebas cubren límites (costo 0, costos `Integer.MAX_VALUE` acumulados en `long`,
dirección de las aristas, ciclos), validación de 50 grafos aleatorios contra
Floyd‑Warshall, cálculos concurrentes, errores de formato con número de línea y
archivos de historial corruptos.

## Diseño

```
src/main/java/com/institucion/rutas/
├── dominio/       Sede, Conexion, MapaSedes, ResultadoRuta, EstadoRuta, excepciones
├── servicio/      CalculadorRutas (interfaz), CalculadorRutasDijkstra, RutaService
├── persistencia/  MapaRepository, HistorialRepository, implementaciones en archivo, FormatoMapa
└── app/           Main (arranque) y Consola (intérprete de comandos)
```

- **Dominio sin dependencias.** `Sede` y `Conexion` son `record`s que se validan en
  su constructor: es imposible crear una conexión con costo negativo o un ID inválido.
  `MapaSedes` valida antes de mutar, así que una operación rechazada no deja estado
  parcial. Entre un par ordenado de sedes hay como máximo una conexión; volver a
  agregarla reemplaza el costo.
- **`SIN_RUTA` es un resultado, no un error.** `ResultadoRuta` lleva el estado
  (`ENCONTRADA`/`SIN_RUTA`), la ruta y el costo (`-1` = `ResultadoRuta.SIN_COSTO` cuando no hay ruta).
  Las entradas inválidas (sede inexistente, ID mal formado, costo negativo) sí son
  excepciones, todas subclases de `MapaException`, que la consola muestra como `ERROR: ...`.
- **Servicio transaccional.** `RutaService` aplica cada cambio sobre una copia del mapa
  y sólo la adopta después de guardarla; si la validación o la escritura fallan, memoria
  y disco quedan como estaban. La importación valida el archivo entero antes de reemplazar.
- **Persistencia en texto plano** (sin base de datos ni servicios externos):
  - `datos-app/mapa.txt`: estado del mapa, mismo formato que importar/exportar.
    Se escribe a un temporal y se mueve de forma atómica.
  - `datos-app/historial.txt`: un registro por cálculo, con entradas y resultado
    (`fecha;origen;destino;estado;costo;ruta`), sólo se agregan líneas al final.
- **Concurrencia.** `MapaSedes` está sincronizado y Dijkstra trabaja sobre una instantánea
  inmutable (`MapaSedes.adyacencia()`), por lo que varios cálculos pueden correr en paralelo.

### Formato de archivo del mapa

```text
# comentario
SEDE;<id>;<nombre>
CONEXION;<origen>;<destino>;<costo>
```

Las sedes deben declararse antes que las conexiones que las usan. Ver `datos/ejemplo-mapa.txt`.

### Complejidad de Dijkstra

- **Tiempo:** O((V + E) · log V). Se usa `PriorityQueue` con *eliminación perezosa*
  (en lugar de decrease‑key, se inserta una entrada nueva y se descartan las obsoletas
  al extraerlas), así que la cola puede tener hasta E entradas: O(E · log E) = O(E · log V).
  Cada sede se procesa una vez. La búsqueda se detiene al extraer el destino.
- **Espacio:** O(V + E): instantánea del grafo, cola, distancias y predecesores.
- **Reconstrucción de ruta:** O(V), siguiendo el mapa de predecesores desde el destino.
- **Desbordamiento:** cada costo cabe en `int` y una ruta mínima tiene a lo sumo V‑1
  aristas, por lo que la suma en `long` no puede desbordar.

## Datos de ejemplo

Todos los datos son ficticios.

- `datos/ejemplo-mapa.txt`: 7 sedes (incluida la aislada `ISLA`) y 9 conexiones;
  incluye el caso del criterio 1.
- `datos/ejemplo-invalido.txt`: archivo con costo `-1` para mostrar el rechazo.
- `datos/demo-comandos.txt`: guion de la demostración.

## Limitaciones conocidas

- Interfaz sólo de consola (el enunciado no exige API HTTP).
- Si hay varias rutas con el mismo costo mínimo se devuelve una de ellas (la primera
  que encuentra el algoritmo), no todas.
- `importar` **reemplaza** el mapa completo; no fusiona con el actual.
- Cada cambio reescribe `mapa.txt` completo: adecuado para cientos o miles de sedes, no
  para mapas masivos. Es de un solo proceso: dos procesos sobre la misma carpeta de datos
  no se coordinan entre sí.
- Los IDs de sede no admiten espacios ni caracteres acentuados; los nombres no admiten `;`.
- El historial crece sin límite; no hay comando para vaciarlo (puede borrarse el archivo).
