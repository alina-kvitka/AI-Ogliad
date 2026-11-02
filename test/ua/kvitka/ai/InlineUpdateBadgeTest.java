package ua.kvitka.ai;
import javax.swing.*;
import java.lang.reflect.*;
public final class InlineUpdateBadgeTest {
  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        NewsWindow window = new NewsWindow();
        Method method = NewsWindow.class.getDeclaredMethod("showNewArticlesBadge", int.class);
        method.setAccessible(true);
        Field field = NewsWindow.class.getDeclaredField("newArticlesBadge");
        field.setAccessible(true);
        JLabel badge = (JLabel) field.get(window);
        method.invoke(window, 2);
        if (!badge.isVisible() || !"+2 нові".equals(badge.getText())) throw new AssertionError("New article count missing");
        method.invoke(window, 0);
        if (!badge.isVisible() || !"0 нових".equals(badge.getText())) throw new AssertionError("Zero refresh count missing");
        System.out.println("PASS persistent inline refresh badge");
      } catch (Exception ex) {throw new RuntimeException(ex);}
    });
  }
}
