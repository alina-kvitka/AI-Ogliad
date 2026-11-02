package ua.kvitka.ai;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/** Керує обраним; змінює стан лише після успішного запису на диск. */
public final class FavoritesService {
    private final Set<String> ids = new HashSet<>();
    private final FavoritesStore store;
    /** Ізольований режим для тестів та знімків інтерфейсу. */
    public FavoritesService() { store = null; }
    public FavoritesService(FavoritesStore store) throws IOException {
        this.store = Objects.requireNonNull(store);
        ids.addAll(store.load());
    }
    public void toggle(String id) {
        if (id == null || id.isBlank() || id.contains("\n") || id.contains("\r"))
            throw new IllegalArgumentException("Некоректний ID новини");
        Set<String> next = new HashSet<>(ids);
        if (!next.remove(id)) next.add(id);
        try { if (store != null) store.save(next); }
        catch (IOException e) { throw new UncheckedIOException(e); }
        ids.clear(); ids.addAll(next);
    }
    public boolean contains(String id) { return ids.contains(id); }
    public Set<String> snapshot() { return Set.copyOf(ids); }
}
