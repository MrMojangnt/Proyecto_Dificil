package com.institucion.rutas.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Utilidades de E/S compartidas por los repositorios. */
final class Archivos {

    private Archivos() {
    }

    /**
     * Escribe el contenido en un archivo temporal y luego lo mueve sobre el
     * destino, para que un fallo a mitad de escritura no deje el archivo truncado.
     */
    static void escribirAtomico(Path destino, String contenido) {
        Path absoluto = destino.toAbsolutePath();
        try {
            Path carpeta = absoluto.getParent();
            Files.createDirectories(carpeta);
            Path temporal = Files.createTempFile(carpeta, absoluto.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temporal, contenido, StandardCharsets.UTF_8);
                try {
                    Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo escribir el archivo " + destino + ": " + e.getMessage(), e);
        }
    }
}
