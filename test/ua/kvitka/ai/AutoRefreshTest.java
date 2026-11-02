package ua.kvitka.ai;
import javax.swing.*;
import java.lang.reflect.*;
public final class AutoRefreshTest {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                NewsWindow window = new NewsWindow();
                Field field=NewsWindow.class.getDeclaredField("autoRefresh");
                field.setAccessible(true);
                Timer timer=(Timer)field.get(window);
                if(timer.getDelay()!=900000 || !timer.isRepeats())throw new AssertionError("15 minute refresh missing");
                Field period=NewsWindow.class.getDeclaredField("period");
                period.setAccessible(true);
                JComboBox<?> choices=(JComboBox<?>)period.get(window);
                if(choices.getItemCount()!=4 || !"За 30 днів".equals(choices.getItemAt(3)))throw new AssertionError("30 day filter missing");
                if(timer.isRunning()) throw new AssertionError("timer runs before window opened");
                System.out.println("PASS: automatic refresh is 15 minutes and lifecycle controlled");
                System.out.println("PASS: 30-day filter available");
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
