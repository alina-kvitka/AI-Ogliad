package ua.kvitka.ai;

import java.util.regex.Pattern;

/** Тематична класифікація: заголовок має більшу вагу за короткий опис. */
public final class NewsClassifier {
    private NewsClassifier() {}
    private static final String[] CATEGORIES={"Штучний інтелект","Робототехніка","Програмування","Технології"};
    private static final Pattern[] TERMS={
        Pattern.compile("(?iu)\\b(?:ai|artificial intelligence|machine learning|deep learning|neural networks?|generative ai|chatgpt|openai|llms?|language models?|gemini|claude|neural|нейромереж\\p{L}*|штучн\\p{L}* інтелект)\\b"),
        Pattern.compile("(?iu)\\b(?:robot\\p{L}*|humanoid\\p{L}*|drone\\p{L}*|robotics|робот\\p{L}*|дрон\\p{L}*)\\b"),
        Pattern.compile("(?iu)\\b(?:programming|programmer\\p{L}*|developer\\p{L}*|coding|codebase|github|gitlab|java|javascript|typescript|python|rust|compilers?|software development|open.source software|програмуванн\\p{L}*|розробник\\p{L}*|код(?:у|ом)?)\\b"),
        Pattern.compile("(?iu)\\b(?:quantum|chip\\p{L}*|processor\\p{L}*|semiconductor\\p{L}*|hardware|smartphone\\p{L}*|gadget\\p{L}*|battery|batteries|computer\\p{L}*|technology|technologies|computing|технологі\\p{L}*|процесор\\p{L}*|квантов\\p{L}*)\\b")
    };
    private static int count(Pattern p,String input){
        if(input==null||input.isBlank())return 0;
        int matches=0;var matcher=p.matcher(input);
        while(matcher.find()&&matches<8)matches++;
        return matches;
    }
    public static String classify(String title,String description,String source){
        int best=3, bestScore=0;
        for(int i=0;i<TERMS.length;i++){
            int score=count(TERMS[i],title)*4+count(TERMS[i],description);
            // Якщо оцінки однакові, залишаємо перший чітко визначений напрям.
            if(score>bestScore || (score>0 && score==bestScore && i==1 && best==0)){bestScore=score;best=i;}
        }
        // Без тематичних ознак застосовуємо загальну категорію, але не назву RSS-джерела.
        return CATEGORIES[best];
    }
    public static NewsArticle normalize(NewsArticle a){
        String cat=classify(a.title(),a.description(),a.source());
        return cat.equals(a.category())?a:new NewsArticle(a.id(),a.title(),a.description(),a.source(),cat,a.date(),a.url());
    }
}
