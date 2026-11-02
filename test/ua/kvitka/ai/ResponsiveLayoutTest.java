package ua.kvitka.ai;

import javax.swing.*;
import java.awt.*;

public final class ResponsiveLayoutTest {
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        System.out.println("PASS: " + description);
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NewsWindow.CardsPanel panel = new NewsWindow.CardsPanel();
            for (int i = 0; i < 6; i++) {
                JPanel card = new JPanel(); card.setPreferredSize(new Dimension(400,186)); panel.add(card);
            }
            JViewport viewport = new JViewport(); viewport.setView(panel);
            viewport.setSize(1000,400); viewport.setExtentSize(new Dimension(1000,400));
            panel.setSize(1000,panel.getPreferredSize().height); panel.doLayout();
            check(((GridLayout)panel.getLayout()).getColumns()==2,"wide layout has 2 columns");
            check(panel.getPreferredSize().height==3*186+2*16+32,"wide layout keeps 186px cards and bottom padding");
            viewport.setSize(600,400); viewport.setExtentSize(new Dimension(600,400));
            panel.setSize(600,panel.getPreferredSize().height);panel.doLayout();
            check(((GridLayout)panel.getLayout()).getColumns()==1,"narrow layout has 1 column");
            check(panel.getPreferredSize().height==6*186+5*16+32,"narrow content scrolls without clipping its bottom");
            check(!panel.getScrollableTracksViewportHeight(),"cards do not stretch to viewport height");
            NewsWindow.Description title = new NewsWindow.Description("An exceptionally long English headline about artificial intelligence in enterprise computing and autonomous robotics worldwide",17,true,2);
            title.setSize(300,title.heightForWidth(300));
            check(title.getHeight()>0,"long English headlines calculate nonzero wrapped height");
        });
    }
}
