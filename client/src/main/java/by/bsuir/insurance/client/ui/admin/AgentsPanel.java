package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.Agent;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Учётные записи страховых агентов (UC14). */
public class AgentsPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<Agent> table = new EntityTable<>(
            new Column<Agent>("ФИО", 260, Agent::getFullName),
            new Column<Agent>("Логин", 110, Agent::getLogin),
            new Column<Agent>("Должность", 200, Agent::getPosition),
            new Column<Agent>("Принят", 100, a -> Ui.format(a.getHireDate())),
            new Column<Agent>("Вознаграждение", 110, a -> a.getCommissionRate().movePointRight(2).stripTrailingZeros()
                    .toPlainString() + " %"),
            new Column<Agent>("Заблокирован", 100, a -> a.isBlocked() ? "да" : "нет"));
    private final JTextField login = new JTextField(14);
    private final JPasswordField password = new JPasswordField(14);
    private final JTextField lastName = new JTextField(18);
    private final JTextField firstName = new JTextField(18);
    private final JTextField patronymic = new JTextField(18);
    private final JTextField position = new JTextField("Страховой агент", 20);
    private final JTextField commission = new JTextField("10", 5);

    public AgentsPanel(Component owner) {
        this.owner = owner;
        Form form = new Form();
        form.add("Логин:", login).add("Пароль:", password).add("Фамилия:", lastName).add("Имя:", firstName)
                .add("Отчество:", patronymic).add("Должность:", position).add("Вознаграждение, %:", commission);
        JButton create = new JButton("Создать учётную запись агента");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(create);
        form.addWide(b);
        add(Ui.title("Страховые агенты"), BorderLayout.NORTH);
        add(table.scroll(900, 280), BorderLayout.CENTER);
        add(Ui.titled(form, "Новый агент"), BorderLayout.SOUTH);
        create.addActionListener(e -> create());
    }

    @Override
    public void refresh() {
        List<Agent> list = Ui.call(owner, Ui.req(CommandType.GET_AGENTS));
        if (list != null) {
            table.setRows(list);
        }
    }

    private void create() {
        try {
            Agent a = new Agent();
            a.setLastName(lastName.getText());
            a.setFirstName(firstName.getText());
            a.setPatronymic(patronymic.getText());
            a.setPosition(position.getText());
            a.setHireDate(LocalDate.now());
            a.setCommissionRate(new BigDecimal(commission.getText().trim().replace(',', '.')).movePointLeft(2));
            if (Ui.callWithMessage(owner, Ui.req(CommandType.CREATE_AGENT).with("login", login.getText())
                    .with("password", new String(password.getPassword())).with("agent", a)) != null) {
                login.setText("");
                password.setText("");
                lastName.setText("");
                firstName.setText("");
                patronymic.setText("");
                refresh();
            }
        } catch (NumberFormatException ex) {
            Ui.error(owner, "Вознаграждение должно быть числом");
        }
    }
}
