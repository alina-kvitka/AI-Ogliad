package ua.kvitka.ai;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import java.awt.*;

public final class SmokeTest {
    static int n;
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);n++;System.out.println("PASS: "+message);}
    static java.util.List<Component> all(Container c){var r=new ArrayList<Component>();for(Component x:c.getComponents()){r.add(x);if(x instanceof Container p)r.addAll(all(p));}return r;}
    public static void main(String[] args)throws Exception{
        LocalDate d=LocalDate.of(2026,10,31);NewsService s=new NewsService(NewsService.demo(d));FavoritesService f=new FavoritesService();
        check(s.search("",null,null,0,false,Set.of(),false,d).size()==4,"all four records");
        check(s.search("JAVA",null,null,0,false,Set.of(),false,d).get(0).id().equals("demo-3"),"case-insensitive description search");
        check(s.search("моделі","Штучний інтелект","Джерело 1",1,false,Set.of(),false,d).size()==1,"combined filters");
        check(s.search("моделі",null,"Джерело 2",0,false,Set.of(),false,d).isEmpty(),"mismatching filters");
        check(s.search("",null,null,7,false,Set.of(),false,d).size()==3,"seven-day range");
        check(s.search("",null,null,0,true,Set.of(),false,d).get(0).id().equals("demo-4"),"oldest first");
        f.toggle("demo-1");check(s.search("",null,null,0,false,f.snapshot(),true,d).size()==1,"favorites filtering");
        f.toggle("demo-1");check(f.snapshot().isEmpty(),"favorite removal");
        SwingUtilities.invokeAndWait(()->{
            NewsWindow w=new NewsWindow();
            var combos=all(w).stream().filter(x->x instanceof JComboBox).map(x->(JComboBox<?>)x).toList();
            check(combos.size()==3 && combos.stream().allMatch(x->x.getUI().getClass().getName().contains("CosmicControls")),"custom combo UI instead of platform painter");
            JComboBox<?> combo=combos.get(0);
            combo.setSelectedIndex(1);
            check(all(w).stream().anyMatch(x->x instanceof JLabel l&&l.getText().startsWith("Знайдено: 2")),"styled dropdown filters results");
            combo.setSelectedIndex(0);
            JButton refresh=(JButton)all(w).stream().filter(x->x instanceof JButton b&&b.getText().contains("Оновити новини")).findFirst().orElseThrow();
            check(refresh instanceof CosmicControls.Button && !refresh.isContentAreaFilled(),"custom rounded button painter");
            JTextField field=(JTextField)all(w).stream().filter(x->x instanceof JTextField).findFirst().orElseThrow();
            field.setText("JAVA");check(all(w).stream().anyMatch(x->x instanceof JLabel l&&l.getText().startsWith("Знайдено: 1")),"UI document listener");
            field.setText("");
            JButton star=(JButton)all(w).stream().filter(x->x instanceof JButton b&&b.getText().equals("☆")).findFirst().orElseThrow();star.doClick();
            JButton fav=(JButton)all(w).stream().filter(x->x instanceof JButton b&&b.getText().equals("Обране")).findFirst().orElseThrow();fav.doClick();
            check(all(w).stream().anyMatch(x->x instanceof JLabel l&&l.getText().startsWith("Знайдено: 1")),"UI favorite and navigation");
            JButton filled=(JButton)all(w).stream().filter(x->x instanceof JButton b&&b.getText().equals("★")).findFirst().orElseThrow();filled.doClick();
            check(all(w).stream().anyMatch(x->x instanceof JLabel l&&l.getText().equals("В обраному поки немає новин")),"UI empty favorites state");
        });System.out.println("TOTAL: "+n+" checks passed");
    }
}
