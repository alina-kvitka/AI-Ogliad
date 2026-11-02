package ua.kvitka.ai;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
public final class RssTest {
    static java.util.List<NewsArticle> parse(String xml)throws Exception{
        return new RssReader().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),new FeedSource("Test","https://example.com/rss"));
    }
    public static void main(String[] args)throws Exception{
        SmokeTest.check(RssReader.date("Mon, 26 Oct 2026 06:06:16 EDT").toString().equals("2026-10-26"),"ScienceDaily EDT date");
        SmokeTest.check(RssReader.date("Sun, 01 Nov 2026 06:06:16 EST").toString().equals("2026-11-01"),"ScienceDaily EST winter date");
        SmokeTest.check(RssReader.date("2026-11-01T10:00:00+01:00").toString().equals("2026-11-01"),"ISO date still supported");
        String item="<item><title>Robot &amp; AI</title><link>https://example.com/news#top</link><description><![CDATA[<p>Hello <b>world</b></p>]]></description><pubDate>Fri, 30 Oct 2026 10:00:00 GMT</pubDate></item>";
        var list=parse("<rss><channel>"+item+item+"<item><title>Bad</title><link>file:///tmp/a</link></item></channel></rss>");
        SmokeTest.check(list.size()==1,"duplicates and unsafe links excluded");
        var a=list.get(0);
        SmokeTest.check(a.title().equals("Robot & AI") && a.description().equals("Hello world"),"HTML cleaned and entities decoded");
        SmokeTest.check(a.date().toString().equals("2026-10-30")&&a.category().equals("Робототехніка"),"publication date and category");
        SmokeTest.check(a.id().equals("https://example.com/news"),"stable ID removes fragment");
        boolean blocked=false;
        try{parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><rss><channel>"+item+"</channel></rss>");}catch(Exception e){blocked=true;}
        SmokeTest.check(blocked,"external XML entities blocked");
        SmokeTest.check(RssReader.date("not a date").equals(java.time.LocalDate.MIN),"unknown date is not presented as today");
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/rss",exchange->{
            byte[] bytes=("<rss><channel>"+item+"</channel></rss>").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);
            try(var output=exchange.getResponseBody()){output.write(bytes);}
        });
        server.createContext("/error",exchange->{exchange.sendResponseHeaders(503,-1);exchange.close();});
        server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();
            SmokeTest.check(new RssReader().fetch(new FeedSource("Local",base+"/rss")).size()==1,"HTTP download and parse end to end");
            boolean failed=false;
            try{new RssReader().fetch(new FeedSource("Local",base+"/error"));}catch(IOException e){failed=true;}
            SmokeTest.check(failed,"HTTP source failure reported");
        }finally{server.stop(0);}
        Path dir=Files.createTempDirectory("rss-test-");
        try{
            NewsCache cache=new NewsCache(dir.resolve("news.xml"));
            SmokeTest.check(cache.load().isEmpty(),"missing cache starts empty");cache.save(list);
            SmokeTest.check(cache.load().equals(list),"cache round trip preserves article and link");
            SwingUtilities.invokeAndWait(()->{
                NewsWindow w=new NewsWindow(new FavoritesService(),cache,list);
                SmokeTest.check(SmokeTest.all(w).stream().anyMatch(c->c instanceof JLabel l&&l.getText().contains("Англійською")),"real news mode label");
            });
        }finally{try(var paths=Files.walk(dir)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
        if(args.length>0 && args[0].equals("--fixtures")) {
            int index=0;
            for(FeedSource source:FeedSource.defaults()) {
                try(InputStream input=Files.newInputStream(Path.of(args[1],(index++)+".xml"))) {
                    var fetched=new RssReader().parse(input,source);
                    SmokeTest.check(!fetched.isEmpty(),"downloaded feed "+source.name()+": "+fetched.size()+" articles");
                    SmokeTest.check(fetched.stream().noneMatch(article->article.date().equals(java.time.LocalDate.MIN)),"all dates parsed: "+source.name());
                }
            }
        }
        if(args.length>0 && args[0].equals("--live"))for(FeedSource source:FeedSource.defaults()){
            var fetched=new RssReader().fetch(source);
            SmokeTest.check(!fetched.isEmpty(),"live "+source.name()+": "+fetched.size()+" articles");
        }
        System.out.println("TOTAL: "+SmokeTest.n+" RSS checks passed");
    }
}
