package by.bsuir.insurance.client.ui;

import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Arrays;

/** Регистрация нового клиента (UC1). */
public class RegisterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JTextField login = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JPasswordField password2 = new JPasswordField(20);
    private final JTextField lastName = new JTextField(20);
    private final JTextField firstName = new JTextField(20);
    private final JTextField patronymic = new JTextField(20);
    private final JTextField birthDate = new JTextField(10);
    private final JTextField passport = new JTextField(12);
    private final JTextField phone = new JTextField("+375 ", 16);
    private final JTextField address = new JTextField(28);
    private User result;

    public RegisterDialog(JFrame owner) {
        super(owner, "Регистрация клиента", true);
        Form f = new Form();
        f.setBorder(Ui.padding(14));
        f.addWide(Ui.title("Учётная запись"));
        f.add("Логин:", login).add("Пароль (не менее 6 символов):", password).add("Повторите пароль:", password2);
        f.addWide(Ui.title("Данные страхователя"));
        f.add("Фамилия:", lastName).add("Имя:", firstName).add("Отчество:", patronymic)
                .add("Дата рождения (дд.мм.гггг):", birthDate).add("Номер паспорта:", passport)
                .add("Телефон:", phone).add("Адрес:", address);
        JButton ok = new JButton("Зарегистрироваться");
        JButton cancel = new JButton("Отмена");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(cancel);
        b.add(ok);
        add(f, BorderLayout.CENTER);
        add(b, BorderLayout.SOUTH);
        ok.addActionListener(e -> register());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(ok);
        pack();
        setLocationRelativeTo(owner);
    }

    public User showDialog() {
        setVisible(true);
        return result;
    }

    private void register() {
        try {
            if (!Arrays.equals(password.getPassword(), password2.getPassword())) {
                throw new IllegalArgumentException("Пароли не совпадают");
            }
            Client c = new Client();
            c.setLastName(lastName.getText());
            c.setFirstName(firstName.getText());
            c.setPatronymic(patronymic.getText());
            c.setBirthDate(Ui.parseDate(birthDate.getText(), "Дата рождения"));
            c.setPassportNo(passport.getText());
            c.setPhone(phone.getText());
            c.setAddress(address.getText());
            result = Ui.call(this, Ui.req(CommandType.REGISTER_CLIENT).with("login", login.getText())
                    .with("password", new String(password.getPassword())).with("client", c));
            if (result != null) {
                dispose();
            }
        } catch (IllegalArgumentException ex) {
            Ui.error(this, ex.getMessage());
        }
    }
}
