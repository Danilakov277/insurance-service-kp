package by.bsuir.insurance.client.ui.agent;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.client.MyPoliciesPanel;
import by.bsuir.insurance.client.ui.client.PolicyColumns;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;
import by.bsuir.insurance.common.model.RefundCalculation;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.util.Money;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

/** Договоры: просмотр, продление (UC9), досрочное расторжение (UC10). */
public class PoliciesPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final JComboBox<Object> status = new JComboBox<>();
    private final EntityTable<Policy> policies = PolicyColumns.agentTable();
    private final EntityTable<Payment> payments = MyPoliciesPanel.paymentsTable();

    public PoliciesPanel(Component owner) {
        this.owner = owner;
        status.addItem("Все договоры");
        for (PolicyStatus s : PolicyStatus.values()) {
            status.addItem(s);
        }
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(Ui.title("Договоры страхования"));
        top.add(new JLabel("   Состояние:"));
        top.add(status);
        JButton renew = new JButton("Продлить");
        JButton terminate = new JButton("Расторгнуть досрочно");
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(renew);
        b.add(terminate);
        bottom.add(new JLabel("График взносов выбранного договора"), BorderLayout.NORTH);
        bottom.add(payments.scroll(600, 140), BorderLayout.CENTER);
        bottom.add(b, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, policies.scroll(900, 320), bottom);
        split.setResizeWeight(0.65);
        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        status.addActionListener(e -> refresh());
        policies.onSelect(p -> payments.setRows(p == null ? null
                : Ui.<List<Payment>>call(owner, Ui.req(CommandType.GET_PAYMENTS).with("policyId", p.getId()))));
        renew.addActionListener(e -> renew());
        terminate.addActionListener(e -> terminate());
    }

    @Override
    public void refresh() {
        Request r = Ui.req(CommandType.GET_POLICIES);
        if (status.getSelectedItem() instanceof PolicyStatus) {
            r.with("status", ((PolicyStatus) status.getSelectedItem()).name());
        }
        List<Policy> list = Ui.call(owner, r);
        if (list != null) {
            policies.setRows(list);
            payments.setRows(null);
        }
    }

    private Policy selected() {
        Policy p = policies.getSelected();
        if (p == null) {
            Ui.error(owner, "Выберите договор");
        }
        return p;
    }

    private void renew() {
        Policy p = selected();
        if (p == null || !Ui.confirm(owner, "Продлить договор " + p.getPolicyNumber() + " на новый срок?")) {
            return;
        }
        Policy n = Ui.call(owner, Ui.req(CommandType.RENEW_POLICY).with("policyId", p.getId()));
        if (n != null) {
            Ui.info(owner, "Оформлен договор-продление " + n.getPolicyNumber() + "\nПериод: "
                    + Ui.format(n.getStartDate()) + " – " + Ui.format(n.getEndDate())
                    + "\nСтраховая премия: " + Money.format(n.getPremium()) + " BYN");
            refresh();
        }
    }

    private void terminate() {
        Policy p = selected();
        if (p == null) {
            return;
        }
        RefundCalculation r = Ui.call(owner, Ui.req(CommandType.PREVIEW_REFUND).with("policyId", p.getId()));
        if (r == null) {
            return;
        }
        String text = "Расторжение договора " + p.getPolicyNumber() + "\n\n"
                + "Уплачено взносов: " + Money.format(r.getPaidPremium()) + " BYN\n"
                + "Неистёкший срок: " + r.getRemainingDays() + " из " + r.getTotalDays() + " дн.\n"
                + "Удержание на расходы: " + r.getExpensesShare().movePointRight(2).stripTrailingZeros().toPlainString() + " %\n"
                + "Произведённые выплаты: " + Money.format(r.getPayouts()) + " BYN\n\n"
                + "Сумма к возврату: " + Money.format(r.getRefund()) + " BYN\n\nРасторгнуть договор?";
        if (Ui.confirm(owner, text)
                && Ui.callWithMessage(owner, Ui.req(CommandType.TERMINATE_POLICY).with("policyId", p.getId())) != null) {
            refresh();
        }
    }
}
