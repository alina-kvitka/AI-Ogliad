package ua.kvitka.ai;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.*;
import java.awt.*;

/** Власне малювання елементів: системна тема macOS не змінює кольори. */
final class CosmicControls {
    private CosmicControls() { }
    static Graphics2D graphics(Graphics g) {
        Graphics2D copy=(Graphics2D)g.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return copy;
    }
    /** Власне поле пошуку: однакове заокруглення незалежно від системної теми. */
    static final class SearchField extends JTextField {
        SearchField(){
            setUI(new BasicTextFieldUI());setOpaque(false);
            setBorder(new EmptyBorder(8,40,8,14));
            addFocusListener(new java.awt.event.FocusAdapter(){
                public void focusGained(java.awt.event.FocusEvent e){repaint();}
                public void focusLost(java.awt.event.FocusEvent e){repaint();}
            });
        }
        protected void paintComponent(Graphics graphics){
            Graphics2D g=graphics(graphics);
            g.setColor(NewsWindow.CARD);g.fillRoundRect(1,1,getWidth()-3,getHeight()-3,14,14);
            g.setColor(hasFocus()?new Color(183,145,250):NewsWindow.LINE);
            g.setStroke(new BasicStroke(hasFocus()?1.6f:1f));g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,14,14);
            int cy=getHeight()/2;g.setColor(NewsWindow.MUTED);g.setStroke(new BasicStroke(1.6f));
            g.drawOval(14,cy-7,11,11);g.drawLine(24,cy+3,29,cy+8);
            super.paintComponent(g);
            if(getText().isEmpty()){
                g.setColor(new Color(154,141,181));g.setFont(getFont());
                FontMetrics fm=g.getFontMetrics();g.drawString("Пошук новин…",40,(getHeight()-fm.getHeight())/2+fm.getAscent());
            }
            g.dispose();
        }
    }
    static class Button extends JButton {
        private final boolean primary;
        Button(String text, boolean primary) {
            super(text); this.primary=primary;
            setUI(new BasicButtonUI() {
                @Override protected void paintText(Graphics g, AbstractButton b, Rectangle rect, String text) {
                    if(b.isEnabled()){super.paintText(g,b,rect,text);return;}
                    g.setColor(new Color(158,146,181));
                    g.drawString(text,rect.x,rect.y+g.getFontMetrics().getAscent());
                }
            }); setOpaque(false); setContentAreaFilled(false);
            setBorderPainted(false); setFocusPainted(false); setRolloverEnabled(true);
            setBorder(new EmptyBorder(11,15,11,15));
            setForeground(NewsWindow.INK);
            setBackground(primary?NewsWindow.ACCENT:new Color(30,22,48));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=graphics(graphics);
            Color base=isEnabled()?getBackground():new Color(29,23,43);
            if(isEnabled() && getModel().isPressed()) base=base.darker();
            else if(isEnabled() && getModel().isRollover()) base=new Color(Math.min(255,base.getRed()+14),Math.min(255,base.getGreen()+12),Math.min(255,base.getBlue()+20));
            if(primary) g.setPaint(new GradientPaint(0,0,base.brighter(),0,getHeight(),base));
            else g.setColor(base);
            g.fillRoundRect(1,1,getWidth()-3,getHeight()-3,14,14);
            g.setColor(hasFocus()?new Color(216,190,255):primary?new Color(165,126,236):NewsWindow.LINE);
            g.setStroke(new BasicStroke(hasFocus()?2f:1f));
            if(!Boolean.TRUE.equals(getClientProperty("navigation")) || Boolean.TRUE.equals(getClientProperty("active")) || hasFocus() || getModel().isRollover()) g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,14,14);
            super.paintComponent(g); g.dispose();
        }
    }
    static final class LineIcon implements Icon {
        private final String kind;
        LineIcon(String kind){this.kind=kind;}
        public int getIconWidth(){return 18;} public int getIconHeight(){return 18;}
        public void paintIcon(Component c,Graphics graphics,int x,int y){
            Graphics2D g=graphics(graphics);g.translate(x,y);g.setColor(c.getForeground());g.setStroke(new BasicStroke(1.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(kind){
                case "Обране" -> {java.awt.geom.Path2D p=new java.awt.geom.Path2D.Double();for(int i=0;i<10;i++){double angle=-Math.PI/2+i*Math.PI/5, r=i%2==0?7:3;double px=9+Math.cos(angle)*r,py=9+Math.sin(angle)*r;if(i==0)p.moveTo(px,py);else p.lineTo(px,py);}p.closePath();g.draw(p);}
                case "Програмування" -> {g.drawLine(6,4,2,9);g.drawLine(2,9,6,14);g.drawLine(12,4,16,9);g.drawLine(16,9,12,14);g.drawLine(10,3,8,15);}
                case "reset" -> {
                    // One continuous return arrow, inset from all icon edges.
                    java.awt.geom.Path2D p=new java.awt.geom.Path2D.Double();
                    p.moveTo(3,7);p.curveTo(4,3,10,2,13,5);
                    p.curveTo(17,9,14,15,9,15);p.curveTo(6,15,4,14,3,12);
                    g.draw(p);
                    p.reset();p.moveTo(3,3);p.lineTo(3,7);p.lineTo(7,7);g.draw(p);
                }
                case "Робототехніка" -> {g.drawRoundRect(2,5,14,11,3,3);g.drawLine(9,2,9,5);g.drawOval(5,8,1,1);g.drawOval(12,8,1,1);g.drawLine(6,13,12,13);}
                case "Штучний інтелект" -> {
                    int[][] nodes={{3,4},{3,14},{9,3},{9,9},{9,15},{15,5},{15,13}};
                    int[][] edges={{0,2},{0,3},{1,3},{1,4},{2,5},{3,5},{3,6},{4,6}};
                    for(int[] e:edges)g.drawLine(nodes[e[0]][0],nodes[e[0]][1],nodes[e[1]][0],nodes[e[1]][1]);
                    for(int[] n:nodes){g.setColor(NewsWindow.BG);g.fillOval(n[0]-2,n[1]-2,4,4);g.setColor(c.getForeground());g.drawOval(n[0]-2,n[1]-2,4,4);}
                }
                case "Технології" -> {g.drawRoundRect(4,4,10,10,2,2);for(int n=6;n<=12;n+=3){g.drawLine(n,1,n,4);g.drawLine(n,14,n,17);g.drawLine(1,n,4,n);g.drawLine(14,n,17,n);}g.drawRect(7,7,4,4);}
                default -> {g.drawRoundRect(2,3,14,12,2,2);g.drawLine(5,6,13,6);g.drawLine(5,9,13,9);g.drawLine(5,12,10,12);}
            }g.dispose();
        }
    }
    static void styleCombo(JComboBox<String> box) {
        box.setOpaque(false); box.setBackground(NewsWindow.CARD); box.setForeground(NewsWindow.INK);
        box.setBorder(new EmptyBorder(2,8,2,3));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel l=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                l.setForeground(NewsWindow.INK);
                l.setBackground(selected?new Color(78,53,130):NewsWindow.CARD);
                l.setOpaque(index>=0); l.setBorder(new EmptyBorder(9,8,9,8));
                return l;
            }
        });
        box.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow=new JButton() {
                    @Override protected void paintComponent(Graphics graphics) {
                        Graphics2D g=graphics(graphics);g.setColor(NewsWindow.MUTED);g.setStroke(new BasicStroke(1.7f));
                        int x=getWidth()/2,y=getHeight()/2;
                        g.drawLine(x-4,y-2,x,y+2);g.drawLine(x,y+2,x+4,y-2);g.dispose();
                    }
                };
                arrow.setOpaque(false);arrow.setContentAreaFilled(false);arrow.setBorder(null);arrow.setFocusable(false);
                arrow.setPreferredSize(new Dimension(30,30));return arrow;
            }
            @Override public void paint(Graphics graphics,JComponent c) {
                Graphics2D g=graphics(graphics);g.setColor(NewsWindow.CARD);
                g.fillRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,12,12);
                g.setColor(c.hasFocus()?new Color(178,142,239):NewsWindow.LINE);
                g.drawRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,12,12);
                super.paint(g,c);g.dispose();
            }
            @Override public void paintCurrentValueBackground(Graphics g,Rectangle bounds,boolean focus) { }
            @Override protected ComboPopup createPopup() {
                BasicComboPopup popup=new BasicComboPopup(comboBox);
                popup.setBorder(BorderFactory.createLineBorder(NewsWindow.LINE));
                popup.getList().setBackground(NewsWindow.CARD);
                popup.getList().setForeground(NewsWindow.INK);
                popup.getList().setSelectionBackground(new Color(78,53,130));
                popup.getList().setSelectionForeground(NewsWindow.INK);
                return popup;
            }
        });
    }
}
