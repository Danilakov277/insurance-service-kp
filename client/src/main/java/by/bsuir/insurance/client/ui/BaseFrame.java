package by.bsuir.insurance.client.ui;

import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Базовое главное окно (расширение JFrame): шапка с именем пользователя и кнопкой выхода,
 * вкладки с рабочими панелями. Наследники добавляют вкладки своей роли.
 */
public abstract class BaseFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    protected final User user;
    protected final JTabbedPane tabs = new JTabbedPane();

    protected BaseFrame(User user, String title) {
        super("Страховой сервис — " + title);
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.ACCENT);
        header.setBorder(Ui.padding(10));
        JLabel caption = new JLabel("Страховой сервис  ·  " + title);
        caption.setForeground(Color.WHITE);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 16f));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel who = new JLabel(user.getDisplayName() + " (" + user.getRole().getTitle() + ")");
        who.setForeground(Color.WHITE);
        JButton logout = new JButton("Выйти");
        logout.addActionListener(e -> logout());
        right.add(who);
        right.add(logout);
        header.add(caption, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        setMinimumSize(new Dimension(1000, 640));
        setSize(1180, 720);
        setLocationRelativeTo(null);
    }

    /** Добавить вкладку; при её выборе панель обновляет данные. */
    protected void addTab(String title, RefreshablePanel panel) {
        tabs.addTab(title, panel);
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == panel) {
                panel.refresh();
            }
        });
    }

    protected void refreshFirstTab() {
        JComponent c = (JComponent) tabs.getComponentAt(0);
        if (c instanceof RefreshablePanel) {
            ((RefreshablePanel) c).refresh();
        }
    }

    private void logout() {
        Ui.call(this, Ui.req(CommandType.LOGOUT));
        dispose();
        new LoginFrame().setVisible(true);
    }

    public User getUser() {
        return user;
    }
}
