package org.institucion.rutas.persistence;

import org.institucion.rutas.domain.Connection;
import org.institucion.rutas.domain.DirectedMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;

public final class MapFileRepository {
    private static final String SITE = "SITE";
    private static final String EDGE = "EDGE";

    public DirectedMap load(Path file) throws IOException {
        DirectedMap map = new DirectedMap();
        if (!Files.exists(file)) {
            return map;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            try {
                if (fields.length == 2 && SITE.equals(fields[0])) {
                    map.addSite(decode(fields[1]));
                } else if (fields.length == 4 && EDGE.equals(fields[0])) {
                    String origin = decode(fields[1]);
                    String destination = decode(fields[2]);
                    int cost = Integer.parseInt(fields[3]);
                    map.addConnection(origin, destination, cost);
                } else {
                    throw new IllegalArgumentException("formato de registro desconocido");
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException("Registro inválido en " + file + ":" + (index + 1)
                        + " (" + exception.getMessage() + ").", exception);
            }
        }
        return map;
    }

    public void save(Path file, DirectedMap map) throws IOException {
        Path absoluteFile = file.toAbsolutePath();
        Path parent = absoluteFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String prefix = absoluteFile.getFileName().toString();
        if (prefix.length() < 3) {
            prefix = (prefix + "___").substring(0, 3);
        }
        Path temporary = Files.createTempFile(parent, prefix, ".tmp");
        try {
            StringBuilder contents = new StringBuilder();
            for (var site : map.sites()) {
                contents.append(SITE).append('\t').append(encode(site.name())).append('\n');
            }
            for (Connection edge : map.connections()) {
                contents.append(EDGE).append('\t').append(encode(edge.origin())).append('\t')
                        .append(encode(edge.destination())).append('\t').append(edge.cost()).append('\n');
            }
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, absoluteFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absoluteFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
