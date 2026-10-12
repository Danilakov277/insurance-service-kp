package by.bsuir.insurance.client.ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/** Базовый класс диаграмм, рисуемых средствами Java2D (расширение JPanel). */
public abstract class ChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /** Палитра серий: спокойные хорошо различимые цвета. */
    protected static final Color[] PALETTE = {
        new Color(0x2F, 0x6B, 0xA8), new Color(0xE0, 0x8E, 0x2B), new Color(0x4C, 0x9A, 0x5A),
        new Color(0xB8, 0x4A, 0x5A), new Color(0x7B, 0x63, 0xA8), new Color(0x8A, 0x8A, 0x8A)
    };

    protected String title = "";
    protected final List<String> labels = new ArrayList<>();

    protected ChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(520, 320));
    }

    public void setTitle(String title) {
        this.title = title;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0x22, 0x22, 0x22));
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(title, 12, 22);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        if (labels.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Нет данных за выбранный период", 12, 50);
        } else {
            drawChart(g2, 12, 36, getWidth() - 24, getHeight() - 48);
        }
        g2.dispose();
    }

    /** Отрисовка диаграммы в заданной области. */
    protected abstract void drawChart(Graphics2D g, int x, int y, int w, int h);

    /** Сокращение подписи до заданной ширины. */
    protected static String fit(String s, FontMetrics fm, int width) {
        if (fm.stringWidth(s) <= width) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > width) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }
}
