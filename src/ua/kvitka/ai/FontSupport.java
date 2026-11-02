package ua.kvitka.ai;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/** Optional OFL font loading from packaged resources or local project fonts/. */
final class FontSupport {
    private static final String SAMPLE = "Українська: і ї є ґ. English: Artificial Intelligence 2026";
    private static final Map<String, Font> LOADED = load();
    private FontSupport() {}

    private static Map<String, Font> load() {
        Map<String, Font> fonts = new HashMap<>();
        load(fonts,"Unbounded", "Unbounded.ttf");
        load(fonts,"Exo 2", "Exo2.ttf");
        return fonts;
    }
    private static void load(Map<String,Font> out,String name,String filename) {
        String resource = "/fonts/" + filename;
        try (InputStream in = FontSupport.class.getResourceAsStream(resource)) {
            if (in != null) register(out,name,Font.createFont(Font.TRUETYPE_FONT,in));
        } catch(Exception ex) { System.err.println("Font resource " + filename + ": " + ex.getMessage()); }
        if(out.containsKey(name)) return;
        Path file = Path.of("fonts",filename);
        if (!Files.isRegularFile(file)) return;
        try(InputStream in=Files.newInputStream(file)) {
            register(out,name,Font.createFont(Font.TRUETYPE_FONT,in));
        } catch(Exception ex) { System.err.println("Font file " + file + ": " + ex.getMessage()); }
    }
    private static void register(Map<String,Font> out,String name,Font f) {
        if (f.canDisplayUpTo(SAMPLE) != -1) {
            System.err.println(name + " lacks some Ukrainian/English glyphs; using fallback.");
            return;
        }
        out.put(name,f);
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
    }
    static Font forUi(int size,boolean bold) {
        Font f=LOADED.get("Exo 2");
        if(f!=null) return f.deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size);
        return fallback(new String[]{"Exo 2","Inter","Helvetica Neue","Segoe UI","Noto Sans"},size,bold);
    }
    static Font forHeadline(int size) {
        Font f=LOADED.get("Unbounded");
        if(f!=null) return f.deriveFont(Font.BOLD,(float)size);
        return fallback(new String[]{"Unbounded","Exo 2","Jura","Noto Sans"},size,true);
    }
    private static Font fallback(String[] choices,int size,boolean bold) {
        Set<String> available=new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for(String family:choices) {
            Font candidate=new Font(family,bold?Font.BOLD:Font.PLAIN,size);
            if(available.contains(family)&&candidate.canDisplayUpTo(SAMPLE)==-1) return candidate;
        }
        return new Font(Font.SANS_SERIF,bold?Font.BOLD:Font.PLAIN,size);
    }
    static boolean supportsBothScripts(Font font) {return font.canDisplayUpTo(SAMPLE)==-1;}
    static String activeUiFamily(){return forUi(14,false).getFamily();}
    static String activeHeadlineFamily(){return forHeadline(18).getFamily();}
}
