package ua.kvitka.ai;

import java.time.LocalDate;
import java.util.*;

/** Пошук, комбіновані фільтри та сортування незалежні від інтерфейсу. */
public final class NewsService {
    private final List<NewsArticle> articles;
    public NewsService(List<NewsArticle> articles) { this.articles = List.copyOf(articles); }
    public List<NewsArticle> search(String query, String category, String source,
                                    int days, boolean oldest, Set<String> favorites,
                                    boolean onlyFavorites, LocalDate today) {
        String q = query.strip().toLowerCase(Locale.ROOT);
        Comparator<NewsArticle> order = Comparator.comparing(NewsArticle::date)
                .thenComparing(NewsArticle::id);
        if (!oldest) order = order.reversed();
        return articles.stream()
                .filter(a -> (a.title() + " " + a.description()).toLowerCase(Locale.ROOT).contains(q))
                .filter(a -> category == null || a.category().equals(category))
                .filter(a -> source == null || a.source().equals(source))
                .filter(a -> days == 0 || (!a.date().isBefore(today.minusDays(days - 1)) && !a.date().isAfter(today)))
                .filter(a -> !onlyFavorites || favorites.contains(a.id()))
                .sorted(order).toList();
    }
    public static List<NewsArticle> demo(LocalDate today) {
        return List.of(
            new NewsArticle("demo-1", "Як мовні моделі допомагають у навчанні", "Навчальний приклад: мовні моделі пояснюють складні теми та допомагають знаходити інформацію.", "Джерело 1", "Штучний інтелект", today),
            new NewsArticle("demo-2", "Роботи навчаються працювати поруч із людьми", "Навчальний приклад: алгоритми допомагають роботам розпізнавати оточення та виконувати спільні завдання.", "Джерело 2", "Робототехніка", today.minusDays(1)),
            new NewsArticle("demo-3", "Нові інструменти для розробників", "Навчальний приклад: тестування Java-програм і пошук помилок у коді стають зручнішими.", "Джерело 3", "Програмування", today.minusDays(3)),
            new NewsArticle("demo-4", "Енергоефективні технології майбутнього", "Навчальний приклад: нові обчислювальні системи оптимізують використання ресурсів та енергії.", "Джерело 1", "Технології", today.minusDays(9))
        );
    }
}
