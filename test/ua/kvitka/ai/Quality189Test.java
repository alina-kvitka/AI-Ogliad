package ua.kvitka.ai;
import java.time.LocalDate;
public final class Quality189Test {
  static void check(boolean b,String label){if(!b)throw new AssertionError(label);System.out.println("PASS "+label);}
  public static void main(String[] args){
    check(NewsClassifier.classify("AI models improve medical diagnosis","Robot technology mentioned in summary","").equals("Штучний інтелект"),"headline AI outweighs robot description");
    check(NewsClassifier.classify("Humanoid robotics advances","AI used for robot control","").equals("Робототехніка"),"headline robot outweighs AI description");
    check(NewsClassifier.classify("Python developers release new compiler","AI powers documentation","").equals("Програмування"),"programming headline wins");
    check(NewsClassifier.classify("Quantum processor breakthrough","AI discussion included","").equals("Технології"),"hardware headline wins");
    check(NewsClassifier.classify("Research weekly roundup","Highlights from scientists","AI Journal").equals("Технології"),"feed name not mistaken for content");
    check(RssReader.date("2026-11-02T08:34:00Z").equals(LocalDate.of(2026,11,2)),"ISO UTC timestamp");
    check(RssReader.date("2026-11-02").equals(LocalDate.of(2026,11,2)),"ISO date without time");
    check(RssReader.date("2026-11-02T08:34:00").equals(LocalDate.of(2026,11,2)),"ISO local timestamp");
    check(RssReader.date("invalid").equals(LocalDate.MIN),"invalid date remains unknown");
  }
}
