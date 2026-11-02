package ua.kvitka.ai;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.ParserDelegator;

/** Завантажує RSS; XML не має доступу до зовнішніх сутностей і локальних файлів. */
public final class RssReader {
    public List<NewsArticle> fetch(FeedSource source) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)URI.create(source.url()).toURL().openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", "AI-Ogliad/1.3 (educational RSS reader)");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
            try (InputStream in = connection.getInputStream()) {
                byte[] bytes = in.readNBytes(2_000_001);
                if (bytes.length > 2_000_000) throw new IOException("Завелика RSS-стрічка");
                return parse(new ByteArrayInputStream(bytes), source);
            }
        } finally { connection.disconnect(); }
    }
    public List<NewsArticle> parse(InputStream input, FeedSource source) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            public void fatalError(SAXParseException e) throws SAXException { throw e; }
            public void error(SAXParseException e) throws SAXException { throw e; }
        });
        Document document = builder.parse(input);
        if (!document.getDocumentElement().getTagName().equals("rss")) throw new IOException("Очікується RSS");
        NodeList items = document.getElementsByTagName("item");
        Map<String,NewsArticle> result = new LinkedHashMap<>();
        for (int i=0; i<Math.min(items.getLength(),150); i++) {
            Element item = (Element)items.item(i);
            String title = plain(text(item,"title")), url = text(item,"link").strip();
            if (title.isBlank() || !safeUrl(url)) continue;
            URI uri = URI.create(url);
            // Фрагмент не впливає на ідентичність публікації.
            url = new URI(uri.getScheme(),uri.getAuthority(),uri.getPath(),uri.getQuery(),null).toString();
            String description = plain(text(item,"description"));
            if (description.length()>280) description=description.substring(0,277)+"…";
            LocalDate date = date(text(item,"pubDate"));
            String category=NewsClassifier.classify(title,description,source.name());
            result.putIfAbsent(url,new NewsArticle(url,title,description,source.name(),category,date,url));
        }
        if (result.isEmpty()) throw new IOException("RSS не містить придатних новин");
        return List.copyOf(result.values());
    }
    static String text(Element e,String tag) {
        NodeList nodes=e.getElementsByTagName(tag);
        return nodes.getLength()==0?"":nodes.item(0).getTextContent();
    }
    static String plain(String html) throws IOException {
        StringBuilder out=new StringBuilder();
        new ParserDelegator().parse(new StringReader(html),new HTMLEditorKit.ParserCallback(){
            public void handleText(char[] data,int position){out.append(data).append(' ');}
        },true);
        return out.toString().replaceAll("\\s+"," ").strip();
    }
    static LocalDate date(String value) {
        // RSS допускає скорочення часових поясів США; Java RFC-парсер очікує числовий зсув.
        String normalized=value.strip();
        Map<String,String> offsets=Map.of("EST","-0500","EDT","-0400",
            "CST","-0600","CDT","-0500","MST","-0700","MDT","-0600",
            "PST","-0800","PDT","-0700","UT","+0000");
        int space=normalized.lastIndexOf(' ');
        if(space>=0){
            String zone=normalized.substring(space+1).toUpperCase(Locale.ROOT);
            if(offsets.containsKey(zone))normalized=normalized.substring(0,space+1)+offsets.get(zone);
        }
        try { return ZonedDateTime.parse(normalized,DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate(); }
        catch (DateTimeParseException ignored) {}
        try { return OffsetDateTime.parse(normalized).toLocalDate(); }
        catch (DateTimeParseException ignored) {}
        try { return LocalDateTime.parse(normalized).toLocalDate(); }
        catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(normalized,DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (DateTimeParseException ignored) {}
        try { return ZonedDateTime.parse(normalized,DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z",Locale.ENGLISH)).toLocalDate(); }
        catch (DateTimeParseException ignored) { return LocalDate.MIN; }
    }
    public static boolean safeUrl(String value) {
        try { URI u=URI.create(value); return ("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme())) && u.getHost()!=null; }
        catch (IllegalArgumentException e) { return false; }
    }
}
