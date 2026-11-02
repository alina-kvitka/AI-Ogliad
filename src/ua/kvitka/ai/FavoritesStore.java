package ua.kvitka.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Читання та безпечна заміна текстового файлу: один ID новини на рядок. */
public final class FavoritesStore {
    private final Path file;
    public FavoritesStore(Path file) { this.file = file.toAbsolutePath(); }
    public Set<String> load() throws IOException {
        try {
            Set<String> ids = new HashSet<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) ids.add(line);
            }
            return ids;
        } catch (NoSuchFileException e) {
            return Set.of(); // Перший запуск: файлу ще немає.
        }
    }
    public void save(Set<String> ids) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "favorites-", ".tmp");
        try {
            Files.write(temporary, new TreeSet<>(ids), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
