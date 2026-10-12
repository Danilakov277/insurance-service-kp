package by.bsuir.insurance.client.ui.client;

import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.Policy;

/** Наборы столбцов таблицы договоров для разных ролей. */
public final class PolicyColumns {

    private PolicyColumns() {
    }

    public static EntityTable<Policy> clientTable() {
        return new EntityTable<>(
                new Column<Policy>("Номер полиса", 130, Policy::getPolicyNumber),
                new Column<Policy>("Вид страхования", 230, Policy::getTypeName),
                new Column<Policy>("Начало", 90, p -> Ui.format(p.getStartDate())),
                new Column<Policy>("Окончание", 90, p -> Ui.format(p.getEndDate())),
                new Column<Policy>("Страховая сумма", 110, Policy::getInsuredSum),
                new Column<Policy>("Премия", 90, Policy::getPremium),
                new Column<Policy>("Уплачено", 90, Policy::getPaidAmount),
                new Column<Policy>("Выплачено", 90, Policy::getPayoutsAmount),
                new Column<Policy>("Состояние", 120, Policy::getStatus));
    }

    public static EntityTable<Policy> agentTable() {
        return new EntityTable<>(
                new Column<Policy>("Номер полиса", 125, Policy::getPolicyNumber),
                new Column<Policy>("Страхователь", 140, Policy::getClientName),
                new Column<Policy>("Вид страхования", 200, Policy::getTypeName),
                new Column<Policy>("Начало", 85, p -> Ui.format(p.getStartDate())),
                new Column<Policy>("Окончание", 85, p -> Ui.format(p.getEndDate())),
                new Column<Policy>("Сумма", 95, Policy::getInsuredSum),
                new Column<Policy>("Премия", 85, Policy::getPremium),
                new Column<Policy>("Уплачено", 85, Policy::getPaidAmount),
                new Column<Policy>("Агент", 110, Policy::getAgentName),
                new Column<Policy>("Состояние", 115, Policy::getStatus));
    }
}
