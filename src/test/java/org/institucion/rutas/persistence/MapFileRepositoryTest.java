package org.institucion.rutas.persistence;

import org.institucion.rutas.domain.DirectedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MapFileRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndReloadsSitesAndConnections() throws IOException {
        Path file = temporaryDirectory.resolve("nested/map.tsv");
        DirectedMap original = new DirectedMap();
        original.addSite("Sede Central");
        original.addSite("Sede Ñ");
        original.addConnection("Sede Central", "Sede Ñ", 12);

        MapFileRepository repository = new MapFileRepository();
        repository.save(file, original);
        DirectedMap loaded = repository.load(file);

        assertEquals(original.sites(), loaded.sites());
        assertEquals(original.connections(), loaded.connections());
    }

    @Test
    void missingFileLoadsAsEmptyMap() throws IOException {
        DirectedMap map = new MapFileRepository().load(temporaryDirectory.resolve("missing.tsv"));
        assertTrue(map.sites().isEmpty());
    }

    @Test
    void invalidFileHasControlledLocationError() throws IOException {
        Path file = temporaryDirectory.resolve("broken.tsv");
        Files.writeString(file, "EDGE\tYQ\tYg\t-1\n");
        IOException error = assertThrows(IOException.class, () -> new MapFileRepository().load(file));
        assertTrue(error.getMessage().contains(":1"));
    }

    @Test
    void malformedRecordIsRejected() throws IOException {
        Path file = temporaryDirectory.resolve("broken.tsv");
        Files.writeString(file, "UNKNOWN\tvalue\n");
        assertThrows(IOException.class, () -> new MapFileRepository().load(file));
    }
}
