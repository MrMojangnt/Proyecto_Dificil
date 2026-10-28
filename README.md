# Rutas de sedes

Aplicación Java 17 de consola para mantener un mapa dirigido de sedes y calcular rutas de costo mínimo. No requiere servicios externos; el estado se guarda localmente en `data/map.tsv`.

## Requisitos y ejecución

Se requiere JDK 17+ y Maven 3.8+. Compilar, probar y crear el JAR ejecutable:

```sh
mvn clean test package
java -jar target/rutas-sedes-1.0.0.jar help
```

Al ejecutar el JAR sin argumentos, se abre una sesión interactiva que espera comandos hasta escribir `salir` o `exit`. Los nombres con espacios se pueden escribir entre comillas:

```sh
java -jar target/rutas-sedes-1.0.0.jar
rutas> add-site "Sede Central"
rutas> list
rutas> salir
```

El mapa persiste entre ejecuciones. Puede cambiarse el archivo usado por los comandos con `MAP_FILE`:

```sh
MAP_FILE=/tmp/sedes.tsv java -jar target/rutas-sedes-1.0.0.jar list
```

## Comandos

```sh
java -jar target/rutas-sedes-1.0.0.jar add-site "Sede Central"
java -jar target/rutas-sedes-1.0.0.jar add-site "Sede Norte"
java -jar target/rutas-sedes-1.0.0.jar add-site "Sede Sur"
java -jar target/rutas-sedes-1.0.0.jar add-connection "Sede Central" "Sede Norte" 4
java -jar target/rutas-sedes-1.0.0.jar add-connection "Sede Central" "Sede Sur" 1
java -jar target/rutas-sedes-1.0.0.jar add-connection "Sede Sur" "Sede Norte" 2
java -jar target/rutas-sedes-1.0.0.jar route "Sede Central" "Sede Norte"
java -jar target/rutas-sedes-1.0.0.jar export copia.tsv
java -jar target/rutas-sedes-1.0.0.jar import copia.tsv
```

La ruta del ejemplo es `Sede Central -> Sede Sur -> Sede Norte`, costo `3`. El resultado para sedes existentes sin conexión alcanzable es `SIN_RUTA`. Los errores de validación se informan en español y no cambian el mapa. Los costos son enteros no negativos; conexiones paralelas entre el mismo par de sedes se reemplazan por el último costo. Los nombres se guardan en UTF-8 codificado en Base64 URL-safe para soportar espacios, tabulaciones y acentos en el formato de archivo.

## Diseño y complejidad

- `domain`: sedes, conexiones y mapa dirigido, con validación de nombres, pesos y existencia de extremos.
- `service`: Dijkstra con `PriorityQueue`, costos `long`, distancias obsoletas descartadas y reconstrucción de ruta.
- `persistence`: importación/exportación del mapa en archivo local; el guardado usa archivo temporal y reemplazo atómico cuando el sistema de archivos lo permite. Al iniciar, el CLI carga el mapa persistido.
- `Main`: interfaz CLI sin dependencias de infraestructura externas.

Con `V` sedes y `E` conexiones, Dijkstra requiere `O((V + E) log V)` tiempo y `O(V + E)` espacio. La aplicación es monoproceso; no ofrece coordinación para escrituras concurrentes entre procesos. Las llamadas al mapa se sincronizan en memoria, pero el archivo local no tiene bloqueo interproceso.

## Pruebas y datos

`mvn test` ejecuta 19 pruebas JUnit 5 de rutas, persistencia, límites y entradas inválidas. No se incluyen sedes precargadas: el mapa comienza vacío y los comandos de ejemplo crean datos ficticios desde cero.
