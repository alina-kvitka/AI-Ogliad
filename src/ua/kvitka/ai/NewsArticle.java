package ua.kvitka.ai;
import java.time.LocalDate;
public record NewsArticle(String id, String title, String description,
                          String source, String category, LocalDate date, String url) {
    public NewsArticle(String id,String title,String description,String source,String category,LocalDate date) {
        this(id,title,description,source,category,date,"");
    }
}
