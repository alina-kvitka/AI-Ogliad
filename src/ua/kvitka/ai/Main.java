package ua.kvitka.ai;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ComboBox.background", new Color(35, 28, 58));
            UIManager.put("ComboBox.foreground", new Color(241, 236, 255));
            UIManager.put("List.background", new Color(35, 28, 58));
            UIManager.put("List.foreground", new Color(241, 236, 255));
            UIManager.put("List.selectionBackground", new Color(85, 59, 143));
            boolean screenshot = args.length == 2 && args[0].equals("--screenshot");
            FavoritesService favorites;
            try {
                favorites = screenshot ? new FavoritesService() : new FavoritesService(
                    new FavoritesStore(Path.of(System.getProperty("user.home"), "AI-Ogliad", "favorites.txt")));
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(null,
                    "Не вдалося прочитати файл обраного.\nПеревірте файл AI-Ogliad/favorites.txt у домашній папці.\nПрограму зупинено, щоб не перезаписати ваші дані.",
                    "Помилка відкриття", JOptionPane.ERROR_MESSAGE);
                return;
            }
            NewsWindow panel;
            if(screenshot) panel=new NewsWindow(favorites);
            else {
                NewsCache cache=new NewsCache(Path.of(System.getProperty("user.home"),"AI-Ogliad","news.xml"));
                java.util.List<NewsArticle> initial;
                try {initial=cache.load();}
                catch(java.io.IOException e){
                    JOptionPane.showMessageDialog(null,"Не вдалося прочитати кеш новин. Перевірте AI-Ogliad/news.xml у домашній папці.\nФайл залишено без змін.","Помилка кешу",JOptionPane.ERROR_MESSAGE);
                    return;
                }
                panel=new NewsWindow(favorites,cache,initial);
            }
            // Відтворюваний знімок реальних Swing-компонентів без монітора.
            if (screenshot) {
                panel.setSize(1440, 940);
                layoutTree(panel);
                BufferedImage image = new BufferedImage(1440, 940, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = image.createGraphics(); panel.printAll(g); g.dispose();
                try { ImageIO.write(image, "png", Path.of(args[1]).toFile()); }
                catch (Exception e) { throw new RuntimeException(e); }
                return;
            }
            JFrame frame = new JFrame("AI Огляд");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(panel);
            Rectangle usable=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setSize(Math.min(1440,usable.width-40), Math.min(940,usable.height-40));
            frame.setMinimumSize(new Dimension(Math.min(1000,usable.width-40),Math.min(720,usable.height-40)));
            frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
    private static void layoutTree(Container c) {
        c.doLayout(); for (Component child : c.getComponents())
            if (child instanceof Container container) layoutTree(container);
    }
}
