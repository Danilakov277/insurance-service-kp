package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.BarChartPanel;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.PieChartPanel;
import by.bsuir.insurance.common.model.LossRatioRow;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.util.Money;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Анализ убыточности видов страхования с диаграммами (UC16). */
public class LossRatioPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final PeriodBar bar = new PeriodBar("Убыточность видов страхования");
    private final EntityTable<LossRatioRow> table = new EntityTable<>(
            new Column<LossRatioRow>("Вид страхования", 260, LossRatioRow::getTypeName),
            new Column<LossRatioRow>("Собрано премий, BYN", 140, LossRatioRow::getPremiums),
            new Column<LossRatioRow>("Выплачено, BYN", 130, LossRatioRow::getPayouts),
            new Column<LossRatioRow>("Убыточность, %", 110, r -> r.getLossRatio() == null ? "—" : r.getLossRatio()));
    private final BarChartPanel bars = new BarChartPanel();
    private final PieChartPanel pie = new PieChartPanel();
    private List<LossRatioRow> rows = new ArrayList<>();

    public LossRatioPanel(Component owner) {
        this.owner = owner;
        JPanel charts = new JPanel(new GridLayout(1, 2, 8, 0));
        charts.add(bars);
        charts.add(pie);
        JPanel center = new JPanel(new BorderLayout(6, 6));
        center.add(table.scroll(900, 180), BorderLayout.NORTH);
        center.add(charts, BorderLayout.CENTER);
        add(bar, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        bar.build.addActionListener(e -> refresh());
        bar.export.addActionListener(e -> export());
    }

    @Override
    public void refresh() {
        try {
            List<LossRatioRow> list = Ui.call(owner, Ui.req(CommandType.LOSS_RATIO_REPORT)
                    .with("from", bar.getFrom()).with("to", bar.getTo()));
            if (list == null) {
                return;
            }
            rows = list;
            table.setRows(list);
            List<String> names = new ArrayList<>();
            List<Double> shares = new ArrayList<>();
            double[] prem = new double[list.size()];
            double[] pay = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                LossRatioRow r = list.get(i);
                names.add(r.getTypeName());
                prem[i] = r.getPremiums().doubleValue();
                pay[i] = r.getPayouts().doubleValue();
                shares.add(prem[i]);
            }
            bars.setTitle("Премии и выплаты, BYN");
            bars.setData(names, Arrays.asList("Премии", "Выплаты"), Arrays.asList(prem, pay));
            pie.setTitle("Структура собранных премий");
            pie.setData(names, shares);
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }

    private void export() {
        List<String[]> data = new ArrayList<>();
        for (LossRatioRow r : rows) {
            data.add(new String[] {r.getTypeName(), Money.format(r.getPremiums()), Money.format(r.getPayouts()),
                    r.getLossRatio() == null ? "—" : r.getLossRatio().toPlainString()});
        }
        ReportExporter.export(owner, "Анализ убыточности видов страхования", bar.periodText(),
                new String[] {"Вид страхования", "Премии, BYN", "Выплаты, BYN", "Убыточность, %"}, data,
                "loss_ratio.txt");
    }
}
