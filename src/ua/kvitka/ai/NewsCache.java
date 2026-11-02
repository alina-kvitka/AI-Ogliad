package ua.kvitka.ai;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** XML Properties зберігає Unicode та переносні дані без Java-серіалізації. */
public final class NewsCache {
    private final Path file;
    public NewsCache(Path file){this.file=file.toAbsolutePath();}
    public List<NewsArticle> load() throws IOException {
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(file)){p.loadFromXML(in);}
        catch(NoSuchFileException e){return List.of();}
        try {
            int n=Integer.parseInt(p.getProperty("count"));
            if(n<0||n>10000)throw new IllegalArgumentException();
            List<NewsArticle> result=new ArrayList<>();
            for(int i=0;i<n;i++){
                String k=i+".";
                String url=Objects.requireNonNull(p.getProperty(k+"url"));
                if(!RssReader.safeUrl(url))throw new IllegalArgumentException();
                result.add(NewsClassifier.normalize(new NewsArticle(p.getProperty(k+"id",url),Objects.requireNonNull(p.getProperty(k+"title")),
                    Objects.requireNonNull(p.getProperty(k+"description")),Objects.requireNonNull(p.getProperty(k+"source")),
                    Objects.requireNonNull(p.getProperty(k+"category")),LocalDate.parse(p.getProperty(k+"date")),url)));
            }
            return ArticleDeduplicator.merge(List.of(),result,Set.of());
        }catch(RuntimeException e){throw new IOException("Пошкоджений кеш новин",e);}
    }
    public void save(List<NewsArticle> articles)throws IOException{
        Properties p=new Properties();p.setProperty("count",Integer.toString(articles.size()));
        for(int i=0;i<articles.size();i++){
            NewsArticle a=articles.get(i);String k=i+".";
            p.setProperty(k+"id",a.id());p.setProperty(k+"url",a.url());p.setProperty(k+"title",a.title());p.setProperty(k+"description",a.description());
            p.setProperty(k+"source",a.source());p.setProperty(k+"category",a.category());p.setProperty(k+"date",a.date().toString());
        }
        Files.createDirectories(file.getParent());Path tmp=Files.createTempFile(file.getParent(),"news-",".tmp");
        try {
            try(OutputStream out=Files.newOutputStream(tmp)){p.storeToXML(out,"AI Огляд — кеш RSS","UTF-8");}
            try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(tmp);}
    }
}
