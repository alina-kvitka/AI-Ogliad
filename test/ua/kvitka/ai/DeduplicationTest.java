package ua.kvitka.ai;
import java.time.LocalDate;
import java.util.*;
import java.nio.file.*;

public final class DeduplicationTest {
    static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
    static NewsArticle article(String id, String url, String title, int day) {
        return new NewsArticle(id,title,"Description", "TechCrunch", "Технології", LocalDate.of(2026,11,day),url);
    }
    public static void main(String[] args) throws Exception {
        String base="https://www.example.com/story/";
        var old=article(base+"?utm_source=rss",base+"?utm_source=rss","A sufficiently long identical title",1);
        var fresh=article(base+"?fbclid=xyz",base+"?fbclid=xyz","A sufficiently long identical title",2);
        var merged=ArticleDeduplicator.merge(List.of(old),List.of(fresh),Set.of(old.id()));
        check(merged.size()==1,"tracked URLs should deduplicate");
        check(merged.get(0).id().equals(old.id()),"favorite ID preserved");
        check(merged.get(0).date().getDayOfMonth()==2,"most recent content retained");
        check(ArticleDeduplicator.canonicalUrl(base+"?b=2&a=1#hi").equals(ArticleDeduplicator.canonicalUrl("http://example.com/story?a=1&b=2")),"query order normalization");
        check(!ArticleDeduplicator.canonicalUrl(base+"?id=1").equals(ArticleDeduplicator.canonicalUrl(base+"?id=2")),"meaningful params retained");
        check(ArticleDeduplicator.merge(List.of(),List.of(article("one","https://example.com/a","Same sufficiently descriptive title",1), article("two","https://other.com/a","Same sufficiently descriptive title",1)),Set.of()).size()==2,"different publishers retained");
        Path temp=Files.createTempDirectory("dedup-test");
        NewsCache cache=new NewsCache(temp.resolve("cache.xml"));
        cache.save(merged);
        check(cache.load().get(0).id().equals(old.id()),"favorite ID persists across restarts");
        Files.deleteIfExists(temp.resolve("cache.xml"));Files.deleteIfExists(temp);
        System.out.println("DeduplicationTest OK");
    }
}
