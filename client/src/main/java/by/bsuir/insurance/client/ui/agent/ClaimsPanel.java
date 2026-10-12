package by.bsuir.insurance.client.ui.agent;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.PayoutDecision;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.util.Money;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.List;

/** Урегулирование страховых случаев (UC8). */
public class ClaimsPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<Claim> table = new EntityTable<>(
            new Column<Claim>("№", 40, Claim::getId),
            new Column<Claim>("Заявлено", 90, c -> Ui.format(c.getReportDate())),
            new Column<Claim>("Полис", 125, Claim::getPolicyNumber),
            new Column<Claim>("Клиент", 130, Claim::getClientName),
            new Column<Claim>("Вид страхования", 190, Claim::getTypeName),
            new Column<Claim>("Дата события", 95, c -> Ui.format(c.getIncidentDate())),
            new Column<Claim>("Описание", 230, Claim::getDescription),
            new Column<Claim>("Ущерб, BYN", 95, Claim::getDamageAmount));
    private final JTextField assessed = new JTextField(10);
    private final JTextField comment = new JTextField(30);
    private final JTextArea steps = new JTextArea(7, 40);

    public ClaimsPanel(Component owner) {
        this.owner = owner;
        steps.setEditable(false);
        Form form = new Form();
        form.add("Оценённый ущерб, BYN:", assessed).add("Комментарий (акт осмотра):", comment);
        JButton calc = new JButton("Рассчитать выплату");
        JButton settle = new JButton("Принять решение");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(calc);
        b.add(settle);
        form.addWide(b);
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.add(Ui.titled(form, "Оценка ущерба"));
        bottom.add(Ui.titled(new JScrollPane(steps), "Обоснование расчёта"));
        add(Ui.title("Заявленные страховые случаи"), BorderLayout.NORTH);
        add(table.scroll(900, 300), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        table.onSelect(c -> {
            assessed.setText(c == null ? "" : c.getDamageAmount().toPlainString());
            steps.setText("");
        });
        calc.addActionListener(e -> preview());
        settle.addActionListener(e -> settle());
    }

    @Override
    public void refresh() {
        List<Claim> list = Ui.call(owner, Ui.req(CommandType.GET_PENDING_CLAIMS));
        if (list != null) {
            table.setRows(list);
            steps.setText("");
        }
    }

    private Claim selected() {
        Claim c = table.getSelected();
        if (c == null) {
            Ui.error(owner, "Выберите страховой случай");
        }
        return c;
    }

    private PayoutDecision preview() {
        Claim c = selected();
        if (c == null) {
            return null;
        }
        try {
            BigDecimal dmg = Ui.parseMoney(assessed.getText(), "Оценённый ущерб");
            PayoutDecision d = Ui.call(owner, Ui.req(CommandType.PREVIEW_PAYOUT)
                    .with("claimId", c.getId()).with("assessedDamage", dmg));
            if (d != null) {
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (String s : d.getSteps()) {
                    sb.append(i++).append(". ").append(s).append('\n');
                }
                sb.append('\n').append(d.isApproved() ? "РЕШЕНИЕ: выплата " + Money.format(d.getAmount()) + " BYN"
                        : "РЕШЕНИЕ: отказ — " + d.getReason());
                steps.setText(sb.toString());
            }
            return d;
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
            return null;
        }
    }

    private void settle() {
        Claim c = table.getSelected();
        PayoutDecision d = preview();
        if (c == null || d == null) {
            return;
        }
        String q = d.isApproved() ? "Утвердить выплату " + Money.format(d.getAmount()) + " BYN?"
                : "Оформить отказ в выплате?";
        if (Ui.confirm(owner, q) && Ui.callWithMessage(owner, Ui.req(CommandType.SETTLE_CLAIM)
                .with("claimId", c.getId()).with("assessedDamage", Ui.parseMoney(assessed.getText(), "Ущерб"))
                .with("comment", comment.getText())) != null) {
            comment.setText("");
            refresh();
        }
    }
}
