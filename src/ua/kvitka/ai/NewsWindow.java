package ua.kvitka.ai;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Вікно першого етапу. Бізнес-логіка винесена в окремі сервіси. */
public final class NewsWindow extends JPanel {
    static final Color BG = new Color(9, 7, 21), CARD = new Color(25, 19, 41),
        INK = new Color(243, 238, 255), MUTED = new Color(187, 174, 218),
        LINE = new Color(56, 41, 80), ACCENT = new Color(116, 77, 201);
    private NewsService news;
    private java.util.List<NewsArticle> articles;
    private final NewsCache cache;
    private boolean updating;
    private final javax.swing.Timer autoRefresh;
    private static final int REFRESH_MS = 15 * 60 * 1000;
    private JButton refresh;
    private final FavoritesService favorites;
    private final JTextField search = new CosmicControls.SearchField();
    private final JComboBox<String> source = new JComboBox<>(new String[]{"Усі джерела", "Джерело 1", "Джерело 2", "Джерело 3"});
    private final JComboBox<String> period = new JComboBox<>(new String[]{"За весь час", "Сьогодні", "За 7 днів", "За 30 днів"});
    private final JComboBox<String> order = new JComboBox<>(new String[]{"Спочатку нові", "Спочатку старі"});
    private final JPanel cards = new CardsPanel();
    private final JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    private final JLabel pageInfo = label("", 12, MUTED, false);
    private final JLabel newArticlesBadge = label("", 12, new Color(190, 157, 244), false);
    private int currentPage;
    private String lastFilterKey = "";
    private static final int PAGE_SIZE = 6;
    private final JLabel count = label("", 13, MUTED, false);
    private final JLabel status = label("", 12, MUTED, false);
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private String category = null, active = "Усі новини";
    private boolean onlyFavorites;

    public NewsWindow() { this(new FavoritesService()); }

    public NewsWindow(FavoritesService favorites) {
        this(favorites, null, NewsService.demo(LocalDate.now()));
    }
    public NewsWindow(FavoritesService favorites, NewsCache cache, java.util.List<NewsArticle> initial) {
        this.favorites = favorites;
        this.cache = cache;
        this.articles = java.util.List.copyOf(initial);
        this.news = new NewsService(articles);
        setLayout(new BorderLayout()); setBackground(BG);
        add(sidebar(), BorderLayout.WEST);
        JPanel main = new StarPanel(); main.setLayout(new BorderLayout(0, 12));
        main.setBorder(new EmptyBorder(18, 24, 18, 24));
        JPanel header = new JPanel(); header.setOpaque(false); header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JPanel title = new JPanel(new BorderLayout()); title.setOpaque(false);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 8)); words.setOpaque(false);
        words.add(headlineLabel("Стрічка новин", 27));
        words.add(label("Усе важливе зі світу технологій", 14, MUTED, false)); title.add(words);
        refresh = button("↻  Оновити новини", true);
        refresh.addActionListener(e -> updateNews());
        JPanel refreshWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); refreshWrap.setOpaque(false); refresh.setPreferredSize(new Dimension(200,46)); refreshWrap.add(refresh); title.add(refreshWrap, BorderLayout.EAST); title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78)); header.add(title);
        header.add(Box.createVerticalStrut(14));
        search.setFont(font(15, false)); search.setForeground(INK); search.setCaretColor(INK); search.setBackground(CARD);

        search.setToolTipText("Пошук за заголовком або описом"); search.getAccessibleContext().setAccessibleName("Пошук новин");
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); header.add(search); header.add(Box.createVerticalStrut(14));
        JPanel filters = new JPanel(new BorderLayout(12,0)); filters.setOpaque(false);
        JPanel selectors=new JPanel(new GridLayout(1,3,12,0));selectors.setOpaque(false);filters.add(selectors);
        for (JComboBox<String> box : java.util.List.of(source, period, order)) {
            box.setFont(font(14, false)); box.setForeground(INK); box.setBackground(CARD); box.setFocusable(true);
            CosmicControls.styleCombo(box); box.addActionListener(e -> render()); selectors.add(box);
        }
        source.getAccessibleContext().setAccessibleName("Джерело"); period.getAccessibleContext().setAccessibleName("Період"); order.getAccessibleContext().setAccessibleName("Сортування");
        JButton reset = button("Скинути фільтри", false); reset.setFont(font(12,true)); reset.setBorder(new EmptyBorder(8,8,8,8)); reset.addActionListener(e -> { search.setText(""); source.setSelectedIndex(0); period.setSelectedIndex(0); order.setSelectedIndex(0); category=null; active=onlyFavorites?"Обране":"Усі новини"; render(); }); filters.add(reset,BorderLayout.EAST); reset.setIcon(new CosmicControls.LineIcon("reset"));reset.setIconTextGap(6);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43)); header.add(filters); header.add(Box.createVerticalStrut(14)); header.add(count);
        for (Component component : header.getComponents()) if (component instanceof JComponent jc) jc.setAlignmentX(LEFT_ALIGNMENT);
        main.add(header, BorderLayout.NORTH);
        cards.setOpaque(false);
        main.add(cards, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(0, 6)); footer.setOpaque(false);
        pagination.setOpaque(false);
        newArticlesBadge.setHorizontalAlignment(SwingConstants.RIGHT);
        newArticlesBadge.setVisible(false);
        JPanel bar = new JPanel(null) {
            public Dimension getPreferredSize() {
                int side = Math.max(pageInfo.getPreferredSize().width, newArticlesBadge.getPreferredSize().width);
                int needed = pagination.getPreferredSize().width + 2 * side + 24;
                return new Dimension(600, getWidth() >= needed ? 40 : 64);
            }
            public void doLayout() {
                int w=getWidth(), pw=pagination.getPreferredSize().width;
                int side=Math.max(pageInfo.getPreferredSize().width, newArticlesBadge.getPreferredSize().width);
                boolean compact=w<pw+2*side+24;
                pagination.setBounds(Math.max(0,(w-pw)/2),0,Math.min(w,pw),34);
                int y=compact?40:0, h=compact?20:34;
                pageInfo.setBounds(0,y,side,h);
                newArticlesBadge.setBounds(Math.max(0,w-side),y,side,h);
            }
        };
        bar.setOpaque(false);
        bar.add(pageInfo);bar.add(pagination);bar.add(newArticlesBadge);
        footer.add(bar); status.setVisible(false); footer.add(status,BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(cards, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10,0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            protected void configureScrollBarColors(){thumbColor=LINE;trackColor=BG;}
            protected JButton createDecreaseButton(int orientation){return tiny();}
            protected JButton createIncreaseButton(int orientation){return tiny();}
            private JButton tiny(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
        });
        main.add(scroll,BorderLayout.CENTER);main.add(footer,BorderLayout.SOUTH);add(main);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){render();} public void removeUpdate(DocumentEvent e){render();} public void changedUpdate(DocumentEvent e){render();}
        });
        if(cache!=null) {
            rebuildSources();
            setStatus(articles.isEmpty()?"Оновіть стрічку, щоб побачити новини.":"");
        }
        render();
        autoRefresh = new javax.swing.Timer(REFRESH_MS, e -> { if (isShowing()) updateNews(); });
        autoRefresh.setRepeats(true);
    }
    @Override public void addNotify() {
        super.addNotify();
        if (cache != null) {
            autoRefresh.start();
            SwingUtilities.invokeLater(this::updateNews);
        }
    }
    @Override public void removeNotify() {
        autoRefresh.stop();
        super.removeNotify();
    }
    @Override public void paint(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        super.paint(g);g.dispose();
    }
    private JPanel sidebar() {
        JPanel p = new StarPanel(); p.setPreferredSize(new Dimension(260, 0));
        p.setBorder(new CompoundBorder(new MatteBorder(0,0,0,1,LINE), new EmptyBorder(36,22,20,22)));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(label("✦  AI Огляд", 27, INK, true)); p.add(Box.createVerticalStrut(9));
        p.add(label("Новини та технології", 13, MUTED, false)); p.add(Box.createVerticalStrut(42));
        nav(p, "Усі новини"); nav(p, "Обране"); p.add(Box.createVerticalStrut(30));
        p.add(label("КАТЕГОРІЇ", 12, MUTED, true)); p.add(Box.createVerticalStrut(16));
        for(String c : new String[]{"Штучний інтелект", "Робототехніка", "Програмування", "Технології"}) nav(p,c);
        p.add(Box.createVerticalGlue()); return p;
    }
    private void nav(JPanel parent, String name) {
        JButton b = button(name, false); b.setAlignmentX(LEFT_ALIGNMENT); b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 49)); b.putClientProperty("navigation",true);b.setIcon(new CosmicControls.LineIcon(name));b.setIconTextGap(10);b.setFont(font(14,false));navigation.put(name,b);
        b.addActionListener(e -> { active=name; onlyFavorites=name.equals("Обране"); category=(name.equals("Усі новини")||onlyFavorites)?null:name; render(); });
        parent.add(b); parent.add(Box.createVerticalStrut(9));
    }
    private void render() {
        if(cards==null) return;
        String filterKey=search.getText()+"\u0000"+category+"\u0000"+source.getSelectedItem()+"\u0000"+period.getSelectedIndex()+"\u0000"+order.getSelectedIndex()+"\u0000"+onlyFavorites;
        if(!filterKey.equals(lastFilterKey)){currentPage=0;lastFilterKey=filterKey;}
        int days = switch(period.getSelectedIndex()) { case 1 -> 1; case 2 -> 7; case 3 -> 30; default -> 0; };
        var results = news.search(search.getText(), category, source.getSelectedIndex()==0?null:(String)source.getSelectedItem(), days, order.getSelectedIndex()==1, favorites.snapshot(), onlyFavorites, LocalDate.now());
        for(var item:navigation.entrySet()){boolean selected=item.getKey().equals(active);item.getValue().putClientProperty("active",selected);item.getValue().setBackground(selected?new Color(74,49,130):new Color(14,10,26));item.getValue().setForeground(selected?INK:MUTED);}
        count.setText("Знайдено: " + results.size() + (cache==null?"":"   •   Англійською")); cards.removeAll();
        int pages=Math.max(1,(results.size()+PAGE_SIZE-1)/PAGE_SIZE);
        currentPage=Math.max(0,Math.min(currentPage,pages-1));
        int first=currentPage*PAGE_SIZE, end=Math.min(first+PAGE_SIZE,results.size());
        pageInfo.setText(results.isEmpty()?"0 новин":(first+1)+"–"+end+" із "+results.size()+" новин");
        buildPagination(pages,results.isEmpty());
        if(results.isEmpty()) {
            JPanel empty = new RoundPanel(); empty.setLayout(new BorderLayout(0,20)); empty.setBorder(new EmptyBorder(36,24,36,24));
            empty.add(label(onlyFavorites?"В обраному поки немає новин":"Новин не знайдено",20,INK,true),BorderLayout.NORTH);
            empty.add(label(onlyFavorites?"Додайте цікаву публікацію до обраного, натиснувши зірочку.":"Спробуйте змінити пошуковий запит або скинути фільтри.",13,MUTED,false));
            JButton emptyReset=button("Скинути фільтри",false);
            emptyReset.addActionListener(e->{search.setText("");source.setSelectedIndex(0);period.setSelectedIndex(0);order.setSelectedIndex(0);category=null;onlyFavorites=false;active="Усі новини";render();});
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));actions.setOpaque(false);actions.add(emptyReset);empty.add(actions,BorderLayout.SOUTH);cards.add(empty);
        } else for(NewsArticle a:results.subList(first,end)) cards.add(card(a));
        while(cards.getComponentCount()<PAGE_SIZE){JPanel blank=new JPanel();blank.setOpaque(false);cards.add(blank);}
        cards.revalidate(); cards.repaint();
    }
    private void buildPagination(int pages, boolean empty) {
        pagination.removeAll();
        pageButton("‹ Назад",currentPage-1,currentPage>0&&!empty,false);
        java.util.TreeSet<Integer> visible=new java.util.TreeSet<>();
        visible.add(0);visible.add(pages-1);
        for(int i=Math.max(0,currentPage-2);i<=Math.min(pages-1,currentPage+2);i++)visible.add(i);
        int previous=-1;
        for(int page:visible){
            if(previous>=0&&page-previous>1)pagination.add(label("…",14,MUTED,false));
            pageButton(Integer.toString(page+1),page,!empty,page==currentPage);
            previous=page;
        }
        pageButton("Далі ›",currentPage+1,currentPage<pages-1&&!empty,false);
        pagination.revalidate();pagination.repaint();
    }
    private void pageButton(String text,int target,boolean enabled,boolean selected) {
        JButton b=button(text,selected);b.setFont(font(12,selected));
        b.setBorder(new EmptyBorder(7,10,7,10));b.setEnabled(enabled);
        b.getAccessibleContext().setAccessibleName("Сторінка: "+text);
        b.addActionListener(e->{currentPage=target;render();cards.scrollRectToVisible(new Rectangle(0,0,1,1));});pagination.add(b);
    }
    
    private JPanel card(NewsArticle a) {
        JPanel c = new RoundPanel(); c.setLayout(new BorderLayout(0,6)); c.setBorder(new EmptyBorder(12,16,12,16)); c.setPreferredSize(new Dimension(400,186));
        JPanel top = new JPanel(new BorderLayout()); top.setOpaque(false);
        JLabel tag=label("•  "+a.category(),12,new Color(190,161,246),true); top.add(tag);
        boolean saved = favorites.contains(a.id());
        JButton star=button(saved?"★":"☆",false); star.setFont(new Font(Font.DIALOG,Font.BOLD,23)); star.setForeground(saved?new Color(205,151,255):new Color(167,138,212));
        star.setBackground(saved?new Color(73,39,110):new Color(30,22,48));
        star.setEnabled(!updating);
        star.setBorder(new EmptyBorder(3,10,3,10)); star.setToolTipText(favorites.contains(a.id())?"Видалити з обраного":"Додати в обране");
        star.getAccessibleContext().setAccessibleName(star.getToolTipText()+": "+a.title()); star.addActionListener(e->{
            try {
                favorites.toggle(a.id());
                
                render();
            } catch (java.io.UncheckedIOException error) {
                setStatus("Не вдалося зберегти обране. Спробуйте ще раз.");
                JOptionPane.showMessageDialog(this,
                    "Не вдалося записати файл обраного.\nПеревірте доступ до папки AI-Ogliad у домашній папці.\nЗміна не застосована.",
                    "Помилка збереження", JOptionPane.ERROR_MESSAGE);
            }
        }); top.add(star,BorderLayout.EAST); c.add(top,BorderLayout.NORTH);
        JPanel body=new JPanel(null) {
            @Override public void doLayout(){
                if(getComponentCount()<2)return;
                Description heading=(Description)getComponent(0);
                int h=heading.heightForWidth(getWidth());
                heading.setBounds(0,0,getWidth(),h);
                getComponent(1).setBounds(0,h+7,getWidth(),Math.max(0,getHeight()-h-7));
            }
        }; body.setOpaque(false);
        Description title=new Description(a.title(),17,true,2); title.setToolTipText(a.title()); body.add(title,BorderLayout.NORTH);
        JComponent description=new Description(a.description(),14,false,2); description.setToolTipText(a.description()); body.add(description); c.add(body);
        JPanel foot=new JPanel(new BorderLayout(6,0)); foot.setOpaque(false);
        foot.add(label(a.source()+"  ·  "+(a.date().equals(LocalDate.MIN)?"Дата невідома":a.date().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),11,MUTED,false));
        JButton read=new GlowReadButton("Докладніше ↗"); read.setFont(font(12,true)); read.setBorder(new EmptyBorder(7,10,7,10));
        read.addActionListener(e -> { if(!a.url().isBlank()){ openArticle(a.url()); return; } JOptionPane.showMessageDialog(this,a.title()+"\n\n"+a.description()+"\n\nЦе навчальний запис, а не справжня публікація.\nПосилання на оригінали додамо разом із RSS-джерелами.","Демонстраційна новина",JOptionPane.INFORMATION_MESSAGE); });
        foot.add(read,BorderLayout.EAST); c.add(foot,BorderLayout.SOUTH); ((RoundPanel)c).setGlowTargets(title,read); ((RoundPanel)c).installHover(c); return c;
    }
    private void rebuildSources() {
        Object selected=source.getSelectedItem();
        source.removeAllItems();source.addItem("Усі джерела");
        java.util.TreeSet<String> names=new java.util.TreeSet<>();
        for(FeedSource f:FeedSource.defaults())names.add(f.name());
        for(NewsArticle a:articles)names.add(a.source());
        for(String name:names)source.addItem(name);
        if(names.contains(selected))source.setSelectedItem(selected);
    }
    // Stable identity for refresh reports; blank URLs must not collapse into one item.
    static String articleRefreshKey(NewsArticle article) {
        String url = ArticleDeduplicator.canonicalUrl(article.url());
        return url.isBlank() ? "id:" + article.id() : "url:" + url;
    }
    private void updateNews() {
        if(cache==null){setStatus("Демонстраційний режим");return;}
        if(updating)return;
        updating=true;refresh.setEnabled(false);refresh.setText("Завантаження…");
        setStatus("Оновлюємо новини…");render();
        var previous=articles;
        var saved=favorites.snapshot();
        new SwingWorker<java.util.List<NewsArticle>,Void>() {
            final java.util.List<String> failures=new ArrayList<>();
            int successful;
            int newArticles;
            protected java.util.List<NewsArticle> doInBackground() throws Exception {
                java.util.List<NewsArticle> incoming = new ArrayList<>();
                java.util.List<NewsArticle> existing = previous.stream().map(NewsClassifier::normalize).toList();
                // One shared deadline; process responsive sources before slow ones.
                FeedLoader.Result result = new FeedLoader().load(FeedSource.defaults());
                incoming.addAll(result.articles());
                // Count genuinely unseen articles, not repeated RSS items.
                java.util.Set<String> known = new java.util.HashSet<>();
                for (NewsArticle a : existing) known.add(articleRefreshKey(a));
                java.util.Set<String> newKeys = new java.util.HashSet<>();
                for (NewsArticle a : incoming) {
                    String key = articleRefreshKey(a);
                    if (!known.contains(key)) newKeys.add(key);
                }
                newArticles = newKeys.size();
                successful=result.successful();
                failures.addAll(result.failures());
                if(successful==0) return previous;
                var sorted=ArticleDeduplicator.merge(existing,incoming,saved).stream()
                    .sorted(Comparator.comparing(NewsArticle::date).reversed()).toList();
                java.util.List<NewsArticle> retained=new ArrayList<>();
                for(NewsArticle a:sorted)if(retained.size()<500||saved.contains(a.id()))retained.add(a);
                cache.save(retained);
                return retained;
            }
            protected void done() {
                updating=false;
                try {
                    articles=get();news=new NewsService(articles);rebuildSources();render();
                    if (successful > 0) showNewArticlesBadge(newArticles);
                    // Only errors occupy the separate footer status row.
                    // Successful refresh messages are temporary and inline with pagination.
                    setStatus(successful == 0
                        ? "Не вдалося оновити новини. Показано збережені матеріали. " + String.join("; ", failures)
                        : failures.isEmpty() ? ""
                            : "Оновлено частково. Недоступні джерела: " + String.join("; ", failures));
                }catch(Exception error){setStatus("Не вдалося зберегти оновлення. Попередні новини залишено.");}
                finally{render();refresh.setEnabled(true);refresh.setText("↻  Оновити новини");}
            }
        }.execute();
    }
    private void showNewArticlesBadge(int added) {
        newArticlesBadge.setText(added > 0 ? "+" + added + " нові" : "0 нових");
        newArticlesBadge.setToolTipText("Додано нових публікацій: " + added);
        newArticlesBadge.setVisible(true);
        revalidate(); repaint();
    }
    private void setStatus(String message) {
        status.setText(message); status.setToolTipText(message);
        status.setVisible(!message.isBlank()); revalidate();
    }
    /** Опис переноситься відповідно до ширини картки, максимум три рядки. */
    static final class Description extends JComponent {
        private final String text; private final int maxLines;
        Description(String text){this(text,13,false,3);}
        Description(String text,int size,boolean bold,int lines){this.text=text;maxLines=lines;setFont(bold?headlineFont(size):font(size,false));setForeground(bold?INK:MUTED);setPreferredSize(new Dimension(100,lines*(getFontMetrics(getFont()).getHeight()+3)));}
        int heightForWidth(int width){
            FontMetrics fm=getFontMetrics(getFont());int start=0,lines=0;
            while(start<text.length() && lines<maxLines){
                int end=start;
                while(end<text.length() && fm.stringWidth(text.substring(start,end+1))<=width-8)end++;
                if(end==start)break;
                if(end<text.length()){int space=text.lastIndexOf(' ',end);if(space>start)end=space;}
                start=end;while(start<text.length() && text.charAt(start)==' ')start++;
                lines++;
            }
            return Math.max(1,lines)*(fm.getHeight()+3);
        }
        protected void paintComponent(Graphics graphics){
            Graphics2D g=CosmicControls.graphics(graphics);g.setFont(getFont());g.setColor(getForeground());
            FontMetrics fm=g.getFontMetrics();int lineHeight=fm.getHeight()+3;
            int lines=Math.min(maxLines,getHeight()/lineHeight), start=0;
            for(int line=0;line<lines && start<text.length();line++){
                int end=start;
                while(end<text.length() && fm.stringWidth(text.substring(start,end+1))<=getWidth()-8)end++;
                if(end==start)break;
                boolean last=line==lines-1 && end<text.length();
                if(last)while(end>start && fm.stringWidth(text.substring(start,end)+"…")>getWidth()-8)end--;
                else if(end<text.length()){int space=text.lastIndexOf(' ',end);if(space>start)end=space;}
                g.drawString(text.substring(start,end).strip()+(last?"…":""),0,line*lineHeight+fm.getAscent());
                start=end;while(start<text.length() && text.charAt(start)==' ')start++;
            }
            g.dispose();
        }
    }
    private void openArticle(String url) {
        try {
            if(!RssReader.safeUrl(url)||!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                throw new java.io.IOException();
            Desktop.getDesktop().browse(java.net.URI.create(url));
        }catch(Exception error){
            JTextField link=new JTextField(url);link.setEditable(false);
            JOptionPane.showMessageDialog(this,new Object[]{"Не вдалося відкрити браузер. Скопіюйте посилання:",link},"Посилання на статтю",JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    static Font headlineFont(int size){return FontSupport.forHeadline(size);}
    static JLabel headlineLabel(String text,int size){JLabel label=label(text,size,INK,true);label.setFont(headlineFont(size));return label;}
    static Font font(int size,boolean bold){return FontSupport.forUi(size,bold);}
    static JLabel label(String text,int size,Color color,boolean bold){JLabel l=new JLabel(text);l.setFont(font(size,bold));l.setForeground(color);return l;}
    static JButton button(String text,boolean primary) {
        JButton b=new CosmicControls.Button(text,primary); b.setFont(font(14,true));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    /** Шість новин на сторінці; одна колонка, якщо дві картки стали б надто вузькими. */
    static final class CardsPanel extends JPanel implements Scrollable {
        private static final int CARD_HEIGHT = 186;
        private static final int H_GAP = 18;
        private static final int V_GAP = 16;
        private static final int BOTTOM_PADDING = 32;
        private static final int TWO_COLUMN_MIN_WIDTH = 760;
        CardsPanel(){super(new GridLayout(0,2,H_GAP,V_GAP));setBorder(new EmptyBorder(0,0,BOTTOM_PADDING,0));setOpaque(false);}
        private int availableWidth(){
            return getParent() instanceof JViewport v ? v.getExtentSize().width : getWidth();
        }
        private int columns(){return availableWidth()>0 && availableWidth()<TWO_COLUMN_MIN_WIDTH ? 1 : 2;}
        private int contentHeight(){
            int rows=(Math.max(1,getComponentCount())+columns()-1)/columns();
            return rows*CARD_HEIGHT+(rows-1)*V_GAP+BOTTOM_PADDING;
        }
        @Override public Dimension getPreferredSize(){return new Dimension(Math.max(320,availableWidth()),contentHeight());}
        @Override public void doLayout(){
            GridLayout layout=(GridLayout)getLayout();
            if(layout.getColumns()!=columns()) layout.setColumns(columns());
            super.doLayout();
        }
        @Override public Dimension getPreferredScrollableViewportSize(){return new Dimension(880,3*CARD_HEIGHT+2*V_GAP+BOTTOM_PADDING);}
        @Override public boolean getScrollableTracksViewportWidth(){return true;}
        // Не розтягуємо 186-px картки на всю висоту екрана.
        @Override public boolean getScrollableTracksViewportHeight(){return false;}
        @Override public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 24;}
        @Override public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(24,visible.height-24);}
    }
    /** Анімація лише малювання: розміри та GridLayout залишаються незмінними. */
    static final class GlowReadButton extends CosmicControls.Button {
        private float glow;
        GlowReadButton(String text){super(text,false);}
        void setGlow(float value){glow=value;repaint();}
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            // Стрілка плавно рухається вправо, не змінюючи ширину кнопки.
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(213,178,255,Math.round(255*glow)));
            g.setFont(getFont());
            FontMetrics fm=g.getFontMetrics();
            int x=getWidth()-getInsets().right-fm.stringWidth("↗")-1;
            int baseline=(getHeight()-fm.getHeight())/2+fm.getAscent();
            g.drawString("↗",x+Math.round(3*glow),baseline);
            g.dispose();
        }
    }
    static final class RoundPanel extends JPanel {
        private boolean hovered;
        private float hoverProgress;
        private javax.swing.Timer animation;
        private Description headline;
        private GlowReadButton link;
        RoundPanel(){setOpaque(false);}
        void setGlowTargets(Description title,JButton read){
            headline=title;
            if(read instanceof GlowReadButton button) link=button;
        }
        void installHover(Component component){
            component.addMouseListener(new java.awt.event.MouseAdapter(){
                public void mouseEntered(java.awt.event.MouseEvent e){setHovered(true);}
                public void mouseExited(java.awt.event.MouseEvent e){
                    java.awt.Point p=SwingUtilities.convertPoint(e.getComponent(),e.getPoint(),RoundPanel.this);
                    setHovered(contains(p));
                }
            });
            if(component instanceof Container container)
                for(Component child:container.getComponents())installHover(child);
        }
        private void setHovered(boolean value){
            if(hovered==value)return;
            hovered=value;
            if(animation!=null)animation.stop();
            // 16 мс × ~16 кадрів = приблизно 250 мс.
            animation=new javax.swing.Timer(16,e->{
                float target=hovered?1f:0f;
                float step=1f/16f;
                hoverProgress=target>hoverProgress?Math.min(target,hoverProgress+step):Math.max(target,hoverProgress-step);
                if(headline!=null){
                    headline.setForeground(blend(INK,Color.WHITE,hoverProgress));
                    headline.repaint();
                }
                if(link!=null)link.setGlow(hoverProgress);
                repaint();
                if(hoverProgress==target)animation.stop();
            });
            animation.start();
        }
        static Color blend(Color a,Color b,float t){
            return new Color(Math.round(a.getRed()+(b.getRed()-a.getRed())*t),
                    Math.round(a.getGreen()+(b.getGreen()-a.getGreen())*t),
                    Math.round(a.getBlue()+(b.getBlue()-a.getBlue())*t));
        }
        @Override protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight();
            if(hoverProgress>0f){
                for(int i=7;i>=2;i--){
                    int alpha=Math.round(hoverProgress*(8-i)*5);
                    g.setColor(new Color(154,95,251,Math.min(45,alpha)));
                    g.setStroke(new BasicStroke(i*1.6f));
                    g.drawRoundRect(4,4,Math.max(1,w-9),Math.max(1,h-9),18,18);
                }
            }
            g.setColor(CARD);g.fillRoundRect(2,3,Math.max(1,w-5),Math.max(1,h-6),18,18);
            g.setColor(blend(LINE,new Color(173,111,255),hoverProgress));
            g.setStroke(new BasicStroke(1f+hoverProgress*.45f));
            g.drawRoundRect(2,3,Math.max(1,w-5),Math.max(1,h-6),18,18);
            g.dispose();super.paintComponent(graphics);
        }
    }
    static final class StarPanel extends JPanel {
        StarPanel(){setOpaque(false);}
        protected void paintComponent(Graphics graphics){
            Graphics2D g=(Graphics2D)graphics.create();g.setColor(BG);g.fillRect(0,0,getWidth(),getHeight());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            boolean sidebar=getWidth()<400;
            int cx=(int)(getWidth()*(sidebar?.35:.85)), cy=(int)(getHeight()*(sidebar?.78:.12));
            g.setPaint(new RadialGradientPaint(new Point(cx,cy),Math.max(220,getWidth()*.65f),new float[]{0,.45f,1},new Color[]{new Color(37,20,66),new Color(20,12,39),BG}));g.fillRect(0,0,getWidth(),getHeight());
            Random r=new Random(28);
            int stars=Math.max(100,getWidth()*getHeight()/2900);
            for(int i=0;i<stars;i++){
                int x=r.nextInt(Math.max(1,getWidth())),y=r.nextInt(Math.max(1,getHeight()));
                boolean bright=i%23==0; int alpha=bright?170:35+r.nextInt(80);
                g.setColor(new Color(201,186,255,alpha));int size=bright?2:1;g.fillOval(x,y,size,size);
                if(bright){g.setColor(new Color(181,150,255,24));g.fillOval(x-3,y-3,8,8);}
                if(i%83==0){g.setColor(new Color(222,208,255,95));g.drawLine(x-3,y+1,x+4,y+1);g.drawLine(x+1,y-3,x+1,y+4);}
            }
            g.dispose();super.paintComponent(graphics);
        }
    }
}
