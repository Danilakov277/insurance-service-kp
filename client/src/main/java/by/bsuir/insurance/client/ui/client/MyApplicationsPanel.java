package by.bsuir.insurance.client.ui.client;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.protocol.CommandType;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

/** Список заявок клиента с их статусами. */
public class MyApplicationsPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<Application> table = new EntityTable<>(
            new Column<Application>("№", 40, Application::getId),
            new Column<Application>("Дата подачи", 120, a -> Ui.format(a.getCreatedAt())),
            new Column<Application>("Вид страхования", 220, Application::getTypeName),
            new Column<Application>("Сумма, BYN", 100, Application::getRequestedSum),
            new Column<Application>("Срок, мес.", 70, Application::getTermMonths),
            new Column<Application>("Объект", 260, Application::getObjectDescription),
            new Column<Application>("Статус", 120, Application::getStatus),
            new Column<Application>("Решение", 260, Application::getDecisionComment));

    public MyApplicationsPanel(Component owner) {
        this.owner = owner;
        add(Ui.title("Мои заявки на страхование"), BorderLayout.NORTH);
        add(table.scroll(900, 400), BorderLayout.CENTER);
        add(Ui.hint("Заявки рассматривает страховой агент. После одобрения договор появится на вкладке "
                + "«Мои договоры и взносы»."), BorderLayout.SOUTH);
    }

    @Override
    public void refresh() {
        List<Application> list = Ui.call(owner, Ui.req(CommandType.GET_MY_APPLICATIONS));
        if (list != null) {
            table.setRows(list);
        }
    }
}
