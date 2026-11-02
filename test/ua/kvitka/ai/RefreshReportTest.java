package ua.kvitka.ai;

import java.time.LocalDate;

public final class RefreshReportTest {
    public static void main(String[] args) {
        NewsArticle a = new NewsArticle("id-1", "Title", "Description", "Source", "Технології", LocalDate.of(2026,11,1), "https://example.org/story?utm_source=email");
        NewsArticle b = new NewsArticle("id-2", "Title", "Description", "Source", "Технології", LocalDate.of(2026,11,1), "https://example.org/story?utm_source=social");
        if (!NewsWindow.articleRefreshKey(a).equals(NewsWindow.articleRefreshKey(b))) throw new AssertionError("Tracking parameters must not count as new story");
        NewsArticle c = new NewsArticle("id-3", "Title", "Description", "Source", "Технології", LocalDate.of(2026,11,1), "");
        NewsArticle d = new NewsArticle("id-4", "Title", "Description", "Source", "Технології", LocalDate.of(2026,11,1), "");
        if (NewsWindow.articleRefreshKey(c).equals(NewsWindow.articleRefreshKey(d))) throw new AssertionError("Blank URLs must retain separate identity");
        System.out.println("PASS refresh new-article identity");
    }
}
