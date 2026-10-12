package by.bsuir.insurance.client.ui;

import by.bsuir.insurance.client.net.ServerConnection;
import by.bsuir.insurance.client.ui.admin.AdminFrame;
import by.bsuir.insurance.client.ui.agent.AgentFrame;
import by.bsuir.insurance.client.ui.client.ClientFrame;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/** Окно входа в систему. */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JTextField login = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);

    public LoginFrame() {
        super("Страховой сервис — вход");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.ACCENT);
        header.setBorder(Ui.padding(16));
        JLabel name = new JLabel("Страховой сервис");
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        JLabel sub = new JLabel("Страхование имущества, КАСКО, несчастных случаев и поездок");
        sub.setForeground(new Color(0xD6, 0xE4, 0xF0));
        header.add(name, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);

        Form form = new Form();
        form.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));
        form.add("Логин:", login).add("Пароль:", password);
        JButton enter = new JButton("Войти");
        JButton register = new JButton("Регистрация клиента");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(register);
        buttons.add(enter);
        form.addWide(buttons);
        JLabel server = new JLabel("Сервер: " + ServerConnection.getInstance().getAddress(), SwingConstants.RIGHT);
        server.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 24));
        server.setForeground(Color.GRAY);

        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(server, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(enter);
        enter.addActionListener(e -> doLogin());
        register.addActionListener(e -> {
            User u = new RegisterDialog(this).showDialog();
            if (u != null) {
                openMain(u);
            }
        });
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        User u = Ui.call(this, Ui.req(CommandType.LOGIN).with("login", login.getText())
                .with("password", new String(password.getPassword())));
        if (u != null) {
            openMain(u);
        }
    }

    /** Открыть главное окно, соответствующее роли пользователя. */
    void openMain(User u) {
        JFrame main;
        switch (u.getRole()) {
            case CLIENT: main = new ClientFrame(u); break;
            case AGENT: main = new AgentFrame(u); break;
            default: main = new AdminFrame(u); break;
        }
        main.setVisible(true);
        dispose();
    }
}
