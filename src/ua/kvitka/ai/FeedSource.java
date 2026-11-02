package ua.kvitka.ai;
import java.util.List;
public record FeedSource(String name, String url) {
    public static List<FeedSource> defaults() {
        return List.of(
            new FeedSource("TechCrunch AI", "https://techcrunch.com/category/artificial-intelligence/feed/"),
            new FeedSource("MIT News AI", "https://news.mit.edu/rss/topic/artificial-intelligence2"),
            new FeedSource("ScienceDaily AI", "https://www.sciencedaily.com/rss/computers_math/artificial_intelligence.xml"),
            new FeedSource("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index"),
            new FeedSource("TechCrunch", "https://techcrunch.com/feed/"),
            new FeedSource("ScienceDaily Computing", "https://www.sciencedaily.com/rss/computers_math/computer_science.xml"));
    }
}
