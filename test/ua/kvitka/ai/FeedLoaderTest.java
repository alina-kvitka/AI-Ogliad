package ua.kvitka.ai;
import java.util.*;
import java.time.*;

public final class FeedLoaderTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static NewsArticle article(String name){return new NewsArticle(name,"Title "+name,"Description",name,"Технології",LocalDate.of(2026,11,2),"https://example.com/"+name);}
    public static void main(String[] args)throws Exception {
        FeedSource slow=new FeedSource("Slow","https://example.com/slow");
        FeedSource good=new FeedSource("Good","https://example.com/good");
        FeedSource bad=new FeedSource("Bad","https://example.com/bad");
        long start=System.nanoTime();
        FeedLoader loader=new FeedLoader(source->{
            if(source.equals(slow)){Thread.sleep(2000);return List.of(article("slow"));}
            if(source.equals(bad))throw new java.io.IOException("HTTP 503");
            return List.of(article("good"));
        },250);
        var result=loader.load(List.of(slow,bad,good));
        long elapsed=java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);
        check(elapsed<1200,"slow feed must not block refresh: "+elapsed);
        check(result.successful()==1 && result.articles().size()==1,"available news must be kept");
        check(result.failures().stream().anyMatch(x->x.contains("Slow")&&x.contains("тайм-аут")),"slow feed reports timeout");
        check(result.failures().stream().anyMatch(x->x.contains("Bad")&&x.contains("503")),"failed feed reports HTTP error");
        var offline=new FeedLoader(f->{throw new java.net.UnknownHostException("offline");},500).load(List.of(good));
        check(offline.successful()==0 && offline.articles().isEmpty(),"offline retains previous data at caller");
        check(offline.failures().get(0).contains("немає доступу"),"offline error is understandable");
        System.out.println("PASS: parallel RSS, shared deadline, partial success, offline error");
    }
}
