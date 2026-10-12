package by.bsuir.insurance.client.ui.components;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/** Круговая диаграмма долей с легендой. */
public class PieChartPanel extends ChartPanel {
    private static final long serialVersionUID = 1L;

    private final List<Double> values = new ArrayList<>();

    public void setData(List<String> names, List<Double> data) {
        labels.clear();
        values.clear();
        for (int i = 0; i < names.size(); i++) {
            if (data.get(i) > 0) {
                labels.add(names.get(i));
                values.add(data.get(i));
            }
        }
        repaint();
    }

    @Override
    protected void drawChart(Graphics2D g, int x, int y, int w, int h) {
        double total = 0;
        for (double v : values) {
            total += v;
        }
        int d = Math.min(h - 10, w / 2 - 10);
        int cx = x + 10;
        int cy = y + (h - d) / 2;
        double start = 90;
        for (int i = 0; i < values.size(); i++) {
            double angle = 360 * values.get(i) / total;
            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillArc(cx, cy, d, d, (int) Math.round(start), (int) -Math.round(angle));
            start -= angle;
        }
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int lx = cx + d + 20;
        int ly = y + 20;
        for (int i = 0; i < values.size(); i++) {
            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillRect(lx, ly - 10, 12, 12);
            g.setColor(Color.DARK_GRAY);
            String pct = String.format("%.1f %%", 100 * values.get(i) / total);
            g.drawString(fit(labels.get(i), fm, x + w - lx - 80) + "  " + pct, lx + 18, ly);
            ly += 22;
        }
    }
}
