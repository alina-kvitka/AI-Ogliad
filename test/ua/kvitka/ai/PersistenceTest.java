package ua.kvitka.ai;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public final class PersistenceTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("ai-og liad-");
        try {
            Path file = directory.resolve("data/favorites.txt");
            FavoritesService first = new FavoritesService(new FavoritesStore(file));
            SmokeTest.check(first.snapshot().isEmpty(), "first run without file");
            first.toggle("demo-1"); first.toggle("новина-2");
            FavoritesService reopened = new FavoritesService(new FavoritesStore(file));
            SmokeTest.check(reopened.snapshot().equals(Set.of("demo-1", "новина-2")), "reopen restores multiple IDs including UTF-8");
            reopened.toggle("новина-2");
            SmokeTest.check(new FavoritesService(new FavoritesStore(file)).snapshot().equals(Set.of("demo-1")), "removal persists");
            SwingUtilities.invokeAndWait(() -> {
                NewsWindow window = new NewsWindow(reopened);
                JButton favorites = (JButton)SmokeTest.all(window).stream().filter(c -> c instanceof JButton b && b.getText().equals("Обране")).findFirst().orElseThrow();
                favorites.doClick();
                SmokeTest.check(SmokeTest.all(window).stream().anyMatch(c -> c instanceof JButton b && b.getText().equals("★")), "restored favorite appears in UI");
            });
            reopened.toggle("demo-1");
            SmokeTest.check(new FavoritesService(new FavoritesStore(file)).snapshot().isEmpty(), "empty selection persists");
            Files.delete(file);
            Files.createDirectory(file);
            Files.writeString(file.resolve("occupied"), "test");
            boolean failed = false;
            try { reopened.toggle("demo-3"); } catch (UncheckedIOException e) { failed = true; }
            SmokeTest.check(failed && reopened.snapshot().isEmpty(), "failed write leaves in-memory selection unchanged");
            boolean loadFailed = false;
            try { new FavoritesService(new FavoritesStore(file)); } catch (IOException e) { loadFailed = true; }
            SmokeTest.check(loadFailed, "unreadable store reported rather than silently discarded");
            try (var paths = Files.list(file.getParent())) {
                SmokeTest.check(paths.noneMatch(p -> p.toString().endsWith(".tmp")), "temporary files cleaned after failed save");
            }
            System.out.println("TOTAL: " + SmokeTest.n + " persistence checks passed");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
}
