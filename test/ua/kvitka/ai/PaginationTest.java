package ua.kvitka.ai;
import javax.swing.*;
import java.time.LocalDate;
import java.util.*;
public final class PaginationTest {
    static JButton button(NewsWindow w,String text){return (JButton)SmokeTest.all(w).stream().filter(c->c instanceof JButton b&&b.getText().equals(text)).findFirst().orElseThrow();}
    static long cards(NewsWindow w){return SmokeTest.all(w).stream().filter(c->c instanceof JButton b&&b.getText().equals("Докладніше ↗")).count();}
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{
            var list=new ArrayList<NewsArticle>();
            for(int i=0;i<41;i++)list.add(new NewsArticle("id-"+i,"Title "+i,"Text",i%2==0?"Джерело 1":"Джерело 2","Технології",LocalDate.now()));
            FavoritesService favorites=new FavoritesService();
            NewsWindow w=new NewsWindow(favorites,null,list);
            SmokeTest.check(cards(w)==6&&!button(w,"‹ Назад").isEnabled(),"first page has six cards and previous disabled");
            NewsWindow.CardsPanel grid=(NewsWindow.CardsPanel)SmokeTest.all(w).stream().filter(c->c instanceof NewsWindow.CardsPanel).findFirst().orElseThrow();
            w.setSize(1440,940);for(int pass=0;pass<4;pass++)PreviewTest.layout(w);
            SmokeTest.check(((java.awt.GridLayout)grid.getLayout()).getColumns()==2 && grid.getComponent(0).getY()<grid.getComponent(2).getY() && grid.getComponent(2).getY()<grid.getComponent(4).getY(),"wide window has two columns and three rows");
            w.setSize(1000,720);for(int pass=0;pass<4;pass++)PreviewTest.layout(w);
            SmokeTest.check(((java.awt.GridLayout)grid.getLayout()).getColumns()==1 && grid.getHeight()>grid.getParent().getHeight(),"narrow window switches to one column and remains scrollable");
            button(w,"Далі ›").doClick();
            SmokeTest.check(SmokeTest.all(w).stream().anyMatch(c->c instanceof JLabel l&&l.getText().startsWith("7–12")),"next page range");
            button(w,"7").doClick();
            SmokeTest.check(cards(w)==5&&!button(w,"Далі ›").isEnabled(),"last partial page and next disabled");
            SmokeTest.check(SmokeTest.all(w).stream().anyMatch(c->c instanceof JLabel l&&l.getText().equals("…")),"large page list uses ellipsis");
            JTextField search=(JTextField)SmokeTest.all(w).stream().filter(c->c instanceof JTextField).findFirst().orElseThrow();
            search.setText("Title 0");
            SmokeTest.check(cards(w)==1&&!button(w,"‹ Назад").isEnabled(),"search returns to first page");
            search.setText("no results");
            SmokeTest.check(cards(w)==0&&!button(w,"Далі ›").isEnabled(),"empty results have no cards or next page");
            search.setText("");
            for(int i=0;i<7;i++)favorites.toggle("id-"+i);
            button(w,"Обране").doClick();button(w,"Далі ›").doClick();
            button(w,"★").doClick();
            SmokeTest.check(cards(w)==6&&!button(w,"‹ Назад").isEnabled(),"removing last favorite on page clamps to previous page");
        });
        System.out.println("TOTAL: "+SmokeTest.n+" pagination checks passed");
    }
}
