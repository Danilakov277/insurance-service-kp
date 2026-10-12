package by.bsuir.insurance.client.ui.client;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

/** Договоры клиента, график взносов и оплата очередного взноса (UC6, UC12). */
public class MyPoliciesPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<Policy> policies = PolicyColumns.clientTable();
    private final EntityTable<Payment> payments = paymentsTable();
    private final JButton pay = new JButton("Внести очередной взнос");

    public MyPoliciesPanel(Component owner) {
        this.owner = owner;
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(Ui.title("График страховых взносов"), BorderLayout.NORTH);
        bottom.add(payments.scroll(600, 160), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(pay);
        bottom.add(buttons, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, policies.scroll(900, 280), bottom);
        split.setResizeWeight(0.6);
        add(Ui.title("Мои договоры страхования"), BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        pay.setEnabled(false);
        policies.onSelect(p -> loadPayments());
        pay.addActionListener(e -> payNext());
    }

    /** Таблица графика взносов (используется также агентом). */
    public static EntityTable<Payment> paymentsTable() {
        return new EntityTable<>(
                new Column<Payment>("№ взноса", 70, Payment::getInstallmentNo),
                new Column<Payment>("Срок уплаты", 110, p -> Ui.format(p.getDueDate())),
                new Column<Payment>("Сумма, BYN", 110, Payment::getAmount),
                new Column<Payment>("Дата оплаты", 110, p -> Ui.format(p.getPaidDate())),
                new Column<Payment>("Состояние", 140, p -> p.isPaid() ? "Уплачен"
                        : (p.isOverdue(LocalDate.now()) ? "Просрочен" : "Ожидает оплаты")));
    }

    @Override
    public void refresh() {
        List<Policy> list = Ui.call(owner, Ui.req(CommandType.GET_MY_POLICIES));
        if (list != null) {
            policies.setRows(list);
            payments.setRows(null);
            pay.setEnabled(false);
        }
    }

    private void loadPayments() {
        Policy p = policies.getSelected();
        if (p == null) {
            payments.setRows(null);
            pay.setEnabled(false);
            return;
        }
        List<Payment> list = Ui.call(owner, Ui.req(CommandType.GET_PAYMENTS).with("policyId", p.getId()));
        payments.setRows(list);
        boolean hasUnpaid = false;
        if (list != null) {
            for (Payment pm : list) {
                hasUnpaid |= !pm.isPaid();
            }
        }
        pay.setEnabled(hasUnpaid);
    }

    private void payNext() {
        Policy p = policies.getSelected();
        if (p == null || !Ui.confirm(owner, "Внести очередной взнос по договору " + p.getPolicyNumber() + "?")) {
            return;
        }
        Policy updated = Ui.callWithMessage(owner, Ui.req(CommandType.PAY_INSTALLMENT).with("policyId", p.getId()));
        if (updated != null) {
            long id = updated.getId();
            refresh();
            for (int i = 0; i < policies.getRowCount(); i++) {
                if (policies.getRows().get(policies.convertRowIndexToModel(i)).getId() == id) {
                    policies.setRowSelectionInterval(i, i);
                }
            }
        }
    }
}
