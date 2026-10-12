package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.BarChartPanel;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.SalesReportRow;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.util.Money;

import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Отчёт о продажах агентов за период с диаграммой (UC15). */
public class SalesReportPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final PeriodBar bar = new PeriodBar("Продажи агентов");
    private final EntityTable<SalesReportRow> table = new EntityTable<>(
            new Column<SalesReportRow>("Агент", 200, SalesReportRow::getAgentName),
            new Column<SalesReportRow>("Договоров", 90, SalesReportRow::getPoliciesCount),
            new Column<SalesReportRow>("Сумма премий, BYN", 140, SalesReportRow::getPremiumSum),
            new Column<SalesReportRow>("Вознаграждение, BYN", 140, SalesReportRow::getCommission));
    private final BarChartPanel chart = new BarChartPanel();
    private List<SalesReportRow> rows = new ArrayList<>();

    public SalesReportPanel(Component owner) {
        this.owner = owner;
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, table.scroll(480, 400), chart);
        split.setResizeWeight(0.45);
        add(bar, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        bar.build.addActionListener(e -> refresh());
        bar.export.addActionListener(e -> export());
    }

    @Override
    public void refresh() {
        try {
            List<SalesReportRow> list = Ui.call(owner, Ui.req(CommandType.SALES_REPORT)
                    .with("from", bar.getFrom()).with("to", bar.getTo()));
            if (list == null) {
                return;
            }
            rows = list;
            table.setRows(list);
            List<String> names = new ArrayList<>();
            double[] premiums = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                names.add(list.get(i).getAgentName());
                premiums[i] = list.get(i).getPremiumSum().doubleValue();
            }
            chart.setTitle("Сумма страховых премий по агентам, BYN");
            chart.setData(names, Collections.singletonList("Премии"), Collections.singletonList(premiums));
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }

    private void export() {
        List<String[]> data = new ArrayList<>();
        for (SalesReportRow r : rows) {
            data.add(new String[] {r.getAgentName(), String.valueOf(r.getPoliciesCount()),
                    Money.format(r.getPremiumSum()), Money.format(r.getCommission())});
        }
        ReportExporter.export(owner, "Отчёт о продажах страховых агентов", bar.periodText(),
                new String[] {"Агент", "Договоров", "Премии, BYN", "Вознаграждение, BYN"}, data, "sales_report.txt");
    }
}
