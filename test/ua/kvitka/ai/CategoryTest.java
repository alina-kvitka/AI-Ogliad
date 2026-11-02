package ua.kvitka.ai;
import java.nio.file.*;
import java.time.*;
import java.util.*;
public final class CategoryTest {
    static void check(boolean ok, String message) { if(!ok) throw new AssertionError(message); System.out.println("PASS: "+message); }
    public static void main(String[] args)throws Exception {
        check(NewsClassifier.classify("New quantum processors", "Faster chips", "TechCrunch").equals("Технології"),"technology news classification");
        check(NewsClassifier.classify("AI model released", "Neural networks", "MIT").equals("Штучний інтелект"),"AI classification");
        check(NewsClassifier.classify("Humanoid robot demonstrated", "Robot motion", "MIT").equals("Робототехніка"),"robotics classification");
        check(NewsClassifier.classify("Python developer tools", "Java programming", "TechCrunch").equals("Програмування"),"programming classification");
        var old=new NewsArticle("https://example.org/x", "New quantum processors", "Faster chips", "TechCrunch AI", "Штучний інтелект",LocalDate.now(),"https://example.org/x");
        Path folder=Files.createTempDirectory("category-test");
        try {
            var cache=new NewsCache(folder.resolve("news.xml"));cache.save(List.of(old));
            var converted=cache.load().get(0);
            check(converted.category().equals("Технології"),"old cache category automatically corrected");
            check(converted.id().equals(old.id()),"favorite identity preserved");
            var results=new NewsService(List.of(converted)).search("","Технології",null,0,false,Set.of(),false,LocalDate.now());
            check(results.size()==1,"technology filter returns article");
        } finally {try(var paths=Files.walk(folder)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
