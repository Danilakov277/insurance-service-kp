package by.bsuir.insurance.client.ui.agent;

import by.bsuir.insurance.client.ui.BaseFrame;
import by.bsuir.insurance.common.model.User;

/** Главное окно страхового агента. */
public class AgentFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    public AgentFrame(User user) {
        super(user, "рабочее место страхового агента");
        addTab("Заявки", new ApplicationsPanel(this));
        addTab("Договоры", new PoliciesPanel(this));
        addTab("Страховые случаи", new ClaimsPanel(this));
        addTab("Контроль", new ControlPanel(this));
        refreshFirstTab();
    }
}
