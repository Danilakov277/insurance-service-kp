package by.bsuir.insurance.client.ui.agent;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.util.Money;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;

/** Рассмотрение заявок: оформление полиса или отказ (UC5). */
public class ApplicationsPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<Application> table = new EntityTable<>(
            new Column<Application>("№", 40, Application::getId),
            new Column<Application>("Поступила", 115, a -> Ui.format(a.getCreatedAt())),
            new Column<Application>("Клиент", 140, Application::getClientName),
            new Column<Application>("Вид страхования", 210, Application::getTypeName),
            new Column<Application>("Сумма, BYN", 95, Application::getRequestedSum),
            new Column<Application>("Срок", 50, Application::getTermMonths),
            new Column<Application>("Франшиза, %", 80, Application::getFranchisePercent),
            new Column<Application>("Взносов", 60, Application::getInstallments),
            new Column<Application>("Расчётная премия", 115, Application::getEstimatedPremium));
    private final JLabel object = new JLabel(" ");
    private final JTextField startDate = new JTextField(10);

    public ApplicationsPanel(Component owner) {
        this.owner = owner;
        JButton approve = new JButton("Одобрить и оформить полис");
        JButton reject = new JButton("Отклонить");
        Form form = new Form();
        form.add("Объект страхования:", object).add("Дата начала договора:", startDate);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(reject);
        b.add(approve);
        form.addWide(b);
        Ui.titled(form, "Решение по заявке");
        add(Ui.title("Заявки, ожидающие рассмотрения"), BorderLayout.NORTH);
        add(table.scroll(900, 380), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
        table.onSelect(a -> object.setText(a == null ? " " : "<html>" + a.getObjectDescription() + "</html>"));
        approve.addActionListener(e -> approve());
        reject.addActionListener(e -> reject());
    }

    @Override
    public void refresh() {
        List<Application> list = Ui.call(owner, Ui.req(CommandType.GET_NEW_APPLICATIONS));
        if (list != null) {
            table.setRows(list);
        }
        startDate.setText(Ui.format(LocalDate.now().plusDays(1)));
    }

    private void approve() {
        Application a = table.getSelected();
        if (a == null) {
            Ui.error(owner, "Выберите заявку");
            return;
        }
        try {
            LocalDate start = Ui.parseDate(startDate.getText(), "Дата начала договора");
            Policy p = Ui.call(owner, Ui.req(CommandType.APPROVE_APPLICATION)
                    .with("applicationId", a.getId()).with("startDate", start));
            if (p != null) {
                Ui.info(owner, "Оформлен договор " + p.getPolicyNumber() + "\nСтраховая премия: "
                        + Money.format(p.getPremium()) + " BYN\nПериод: " + Ui.format(p.getStartDate()) + " – "
                        + Ui.format(p.getEndDate()));
                refresh();
            }
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }

    private void reject() {
        Application a = table.getSelected();
        if (a == null) {
            Ui.error(owner, "Выберите заявку");
            return;
        }
        String reason = JOptionPane.showInputDialog(owner, "Причина отказа:", "Отклонение заявки",
                JOptionPane.QUESTION_MESSAGE);
        if (reason != null && Ui.callWithMessage(owner, Ui.req(CommandType.REJECT_APPLICATION)
                .with("applicationId", a.getId()).with("comment", reason)) != null) {
            refresh();
        }
    }
}
