package by.bsuir.insurance.client.ui.components;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Столбчатая диаграмма с несколькими сериями (столбцы групп стоят рядом). */
public class BarChartPanel extends ChartPanel {
    private static final long serialVersionUID = 1L;

    private final List<String> seriesNames = new ArrayList<>();
    private final List<double[]> series = new ArrayList<>();

    /** Задать категории и значения серий: values.get(s)[i] — значение серии s для категории i. */
    public void setData(List<String> categories, List<String> names, List<double[]> values) {
        labels.clear();
        labels.addAll(categories);
        seriesNames.clear();
        seriesNames.addAll(names);
        series.clear();
        series.addAll(values);
        repaint();
    }

    @Override
    protected void drawChart(Graphics2D g, int x, int y, int w, int h) {
        FontMetrics fm = g.getFontMetrics();
        double max = 0;
        for (double[] s : series) {
            for (double v : s) {
                max = Math.max(max, v);
            }
        }
        max = niceMax(max);
        int legendH = seriesNames.size() > 1 ? 20 : 0;
        int left = x + 70;
        int bottom = y + h - 34 - legendH;
        int top = y + 8;
        int plotW = x + w - left;
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols(new java.util.Locale("ru", "BY"));
        sym.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0", sym);
        // сетка и ось значений
        for (int i = 0; i <= 4; i++) {
            double v = max * i / 4;
            int yy = bottom - (int) ((bottom - top) * i / 4.0);
            g.setColor(new Color(0xE6, 0xE6, 0xE6));
            g.drawLine(left, yy, left + plotW, yy);
            g.setColor(Color.DARK_GRAY);
            String s = df.format(v);
            g.drawString(s, left - 6 - fm.stringWidth(s), yy + 4);
        }
        int n = labels.size();
        int groupW = plotW / Math.max(1, n);
        int barW = Math.max(6, Math.min(48, (int) (groupW * 0.7 / Math.max(1, series.size()))));
        for (int i = 0; i < n; i++) {
            int gx = left + i * groupW + (groupW - barW * series.size()) / 2;
            for (int s = 0; s < series.size(); s++) {
                double v = series.get(s)[i];
                int bh = max == 0 ? 0 : (int) ((bottom - top) * v / max);
                g.setColor(PALETTE[s % PALETTE.length]);
                g.fillRect(gx + s * barW, bottom - bh, barW - 2, bh);
            }
            g.setColor(Color.DARK_GRAY);
            String lab = fit(labels.get(i), fm, groupW - 4);
            g.drawString(lab, left + i * groupW + (groupW - fm.stringWidth(lab)) / 2, bottom + 16);
        }
        g.setColor(Color.GRAY);
        g.drawLine(left, bottom, left + plotW, bottom);
        // легенда
        if (legendH > 0) {
            int lx = left;
            int ly = y + h - 10;
            for (int s = 0; s < seriesNames.size(); s++) {
                g.setColor(PALETTE[s % PALETTE.length]);
                g.fillRect(lx, ly - 10, 12, 12);
                g.setColor(Color.DARK_GRAY);
                g.drawString(seriesNames.get(s), lx + 16, ly);
                lx += 16 + fm.stringWidth(seriesNames.get(s)) + 24;
            }
        }
    }

    /** Округление верхней границы шкалы до «круглого» числа. */
    static double niceMax(double v) {
        if (v <= 0) {
            return 1;
        }
        double p = Math.pow(10, Math.floor(Math.log10(v)));
        double[] steps = {1, 2, 2.5, 5, 10};
        for (double s : steps) {
            if (v <= s * p) {
                return s * p;
            }
        }
        return 10 * p;
    }
}
