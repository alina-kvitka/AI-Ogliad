package ua.kvitka.ai;
public final class FontSupportTest {
    public static void main(String[] args) {
        if(!FontSupport.supportsBothScripts(FontSupport.forUi(14,false))) throw new AssertionError("UI font missing glyphs");
        if(!FontSupport.supportsBothScripts(FontSupport.forHeadline(18))) throw new AssertionError("Headline font missing glyphs");
        System.out.println("Font glyphs OK; UI="+FontSupport.activeUiFamily()+"; titles="+FontSupport.activeHeadlineFamily());
    }
}
