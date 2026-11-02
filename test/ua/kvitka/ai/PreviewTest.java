package ua.kvitka.ai;
import java.nio.file.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.imageio.ImageIO;
public final class PreviewTest {
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container p)layout(p);}
    public static void main(String[] args)throws Exception{
        java.util.List<NewsArticle> articles;
        try(var in=Files.newInputStream(Path.of(args[0]))){articles=new RssReader().parse(in,FeedSource.defaults().get(0));}
        SwingUtilities.invokeAndWait(()->{
            NewsWindow panel=new NewsWindow(new FavoritesService(),new NewsCache(Path.of("unused-preview.xml")),articles);
            int width=args.length>2?Integer.parseInt(args[2]):1440;
            int height=args.length>3?Integer.parseInt(args[3]):940;
            panel.setSize(width,height);for(int pass=0;pass<4;pass++)layout(panel);
            BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
            try{ImageIO.write(image,"png",Path.of(args[1]).toFile());}catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
