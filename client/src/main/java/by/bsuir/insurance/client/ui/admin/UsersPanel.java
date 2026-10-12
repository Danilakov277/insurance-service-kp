package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

/** Блокировка и разблокировка учётных записей (UC14). */
public class UsersPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<User> table = new EntityTable<>(
            new Column<User>("Логин", 140, User::getLogin),
            new Column<User>("Имя", 260, User::getDisplayName),
            new Column<User>("Роль", 160, u -> u.getRole().getTitle()),
            new Column<User>("Зарегистрирован", 140, u -> Ui.format(u.getCreatedAt())),
            new Column<User>("Состояние", 120, u -> u.isBlocked() ? "Заблокирован" : "Активен"));

    public UsersPanel(Component owner) {
        this.owner = owner;
        JButton block = new JButton("Заблокировать");
        JButton unblock = new JButton("Разблокировать");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(unblock);
        b.add(block);
        add(Ui.title("Учётные записи пользователей"), BorderLayout.NORTH);
        add(table.scroll(900, 450), BorderLayout.CENTER);
        add(b, BorderLayout.SOUTH);
        block.addActionListener(e -> setBlocked(true));
        unblock.addActionListener(e -> setBlocked(false));
    }

    @Override
    public void refresh() {
        List<User> list = Ui.call(owner, Ui.req(CommandType.GET_USERS));
        if (list != null) {
            table.setRows(list);
        }
    }

    private void setBlocked(boolean blocked) {
        User u = table.getSelected();
        if (u == null) {
            Ui.error(owner, "Выберите пользователя");
            return;
        }
        if (Ui.callWithMessage(owner, Ui.req(CommandType.SET_USER_BLOCKED).with("userId", u.getId())
                .with("blocked", blocked)) != null) {
            refresh();
        }
    }
}
