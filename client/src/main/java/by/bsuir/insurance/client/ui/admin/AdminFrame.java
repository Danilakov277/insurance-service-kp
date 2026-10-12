package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.BaseFrame;
import by.bsuir.insurance.common.model.User;

/** Главное окно администратора. */
public class AdminFrame extends BaseFrame {
    private static final long serialVersionUID = 1L;

    public AdminFrame(User user) {
        super(user, "администрирование");
        addTab("Виды страхования", new TypesPanel(this));
        addTab("Агенты", new AgentsPanel(this));
        addTab("Пользователи", new UsersPanel(this));
        addTab("Продажи агентов", new SalesReportPanel(this));
        addTab("Убыточность", new LossRatioPanel(this));
        refreshFirstTab();
    }
}
