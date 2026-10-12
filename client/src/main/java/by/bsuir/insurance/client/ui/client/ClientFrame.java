package by.bsuir.insurance.client.ui.client;

import by.bsuir.insurance.client.ui.BaseFrame;
import by.bsuir.insurance.common.model.User;

/** Главное окно клиента. */
public class ClientFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    public ClientFrame(User user) {
        super(user, "личный кабинет клиента");
        MyPoliciesPanel policies = new MyPoliciesPanel(this);
        addTab("Расчёт и заявка", new CalculatorPanel(this));
        addTab("Мои заявки", new MyApplicationsPanel(this));
        addTab("Мои договоры и взносы", policies);
        addTab("Страховые случаи", new MyClaimsPanel(this));
        refreshFirstTab();
    }
}
