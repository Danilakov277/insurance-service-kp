package by.bsuir.insurance.client;

import by.bsuir.insurance.client.net.ServerConnection;
import by.bsuir.insurance.client.ui.LoginFrame;
import by.bsuir.insurance.client.ui.RegisterDialog;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.admin.AdminFrame;
import by.bsuir.insurance.client.ui.agent.AgentFrame;
import by.bsuir.insurance.client.ui.client.ClientFrame;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Автоматический обход окон клиентского приложения со снимками экрана
 * (для пояснительной записки и плаката). Требует запущенного сервера с демо-данными.
 */
public final class ScreenshotTour {
    private static File dir;
    private static Robot robot;

    private ScreenshotTour() {
    }

    public static void main(String[] args) throws Exception {
        dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        robot = new Robot();
        ServerConnection.configure("localhost", 5555);
        SwingUtilities.invokeAndWait(ClientApp::setupLookAndFeel);

        // ---- вход
        JFrame login = edt(() -> {
            LoginFrame f = new LoginFrame();
            f.setLocation(20, 20);
            f.setVisible(true);
            return f;
        });
        on(() -> {
            texts(login).get(0).setText("ivanov");
            texts(login).get(1).setText("client123");
        });
        shot(login, "01_login");
        on(login::dispose);

        // ---- регистрация
        JDialog reg = edt(() -> {
            RegisterDialog d = new RegisterDialog(null);
            d.setModal(false);
            d.setLocation(20, 20);
            List<JTextField> t = texts(d);
            String[] v = {"petrova", "secret1", "secret1", "Петрова", "Алина", "Сергеевна", "21.06.1999",
                "MP4567812", "+375 29 765-43-21", "г. Минск, ул. Кирова, 12-5"};
            for (int i = 0; i < v.length; i++) {
                t.get(i).setText(v[i]);
            }
            d.setVisible(true);
            return d;
        });
        shot(reg, "02_register");
        on(reg::dispose);

        // ---- клиент
        JFrame client = open("ivanov", "client123");
        JTabbedPane tabs = find(client, JTabbedPane.class).get(0);
        on(() -> {
            List<JComboBox> boxes = find(client, JComboBox.class);
            boxes.get(0).setSelectedIndex(2);
            texts(client).get(0).setText("30000");
            find(client, JTextArea.class).get(0).setText("Volkswagen Polo, 2019 г.в., госномер 1234 AB-7");
            boxes.get(2).setSelectedItem(2);
            boxes.get(3).setSelectedItem(4);
            button(client, "Рассчитать стоимость").doClick();
        });
        shot(client, "03_client_calculator");
        tab(tabs, 1);
        shot(client, "04_client_applications");
        tab(tabs, 2);
        selectRow((Container) tabs.getSelectedComponent(), 0, 0);
        shot(client, "05_client_policies");
        tab(tabs, 3);
        on(() -> {
            JTextField dmg = texts(tabs.getSelectedComponent()).get(1);
            dmg.setText("850");
            find((Container) tabs.getSelectedComponent(), JTextArea.class).get(0).setText("Повреждение лобового стекла камнем на трассе М1");
        });
        shot(client, "06_client_claims");
        on(client::dispose);

        // ---- агент
        JFrame agent = open("kovaleva", "agent123");
        JTabbedPane at = find(agent, JTabbedPane.class).get(0);
        selectRow((Container) at.getSelectedComponent(), 0, 0);
        shot(agent, "07_agent_applications");
        tab(at, 1);
        selectRow((Container) at.getSelectedComponent(), 0, 2);
        shot(agent, "08_agent_policies");
        tab(at, 2);
        selectRow((Container) at.getSelectedComponent(), 0, 0);
        on(() -> button((Container) at.getSelectedComponent(), "Рассчитать выплату").doClick());
        shot(agent, "09_agent_claims");
        tab(at, 3);
        shot(agent, "10_agent_control");
        on(agent::dispose);

        // ---- администратор
        JFrame admin = open("admin", "admin123");
        JTabbedPane ad = find(admin, JTabbedPane.class).get(0);
        selectRow((Container) ad.getSelectedComponent(), 0, 2);
        shot(admin, "11_admin_types");
        tab(ad, 1);
        shot(admin, "12_admin_agents");
        tab(ad, 2);
        shot(admin, "13_admin_users");
        tab(ad, 3);
        on(() -> {
            List<JTextField> t = texts((Container) ad.getSelectedComponent());
            t.get(0).setText("01.01.2026");
            button((Container) ad.getSelectedComponent(), "Сформировать").doClick();
        });
        shot(admin, "14_admin_sales");
        tab(ad, 4);
        on(() -> {
            List<JTextField> t = texts((Container) ad.getSelectedComponent());
            t.get(0).setText("01.01.2025");
            button((Container) ad.getSelectedComponent(), "Сформировать").doClick();
        });
        shot(admin, "15_admin_loss_ratio");
        on(admin::dispose);
        System.exit(0);
    }

    private static JFrame open(String login, String password) throws Exception {
        User u = edt(() -> Ui.call(null, Ui.req(CommandType.LOGIN).with("login", login).with("password", password)));
        JFrame f = edt(() -> {
            JFrame fr;
            switch (u.getRole()) {
                case CLIENT: fr = new ClientFrame(u); break;
                case AGENT: fr = new AgentFrame(u); break;
                default: fr = new AdminFrame(u); break;
            }
            fr.setSize(1180, 720);
            fr.setLocation(20, 20);
            fr.setVisible(true);
            return fr;
        });
        Thread.sleep(800);
        return f;
    }

    private static void tab(JTabbedPane tabs, int i) throws Exception {
        on(() -> tabs.setSelectedIndex(i));
    }

    private static void selectRow(Container c, int table, int row) throws Exception {
        on(() -> {
            JTable t = find(c, JTable.class).get(table);
            if (t.getRowCount() > row) {
                t.setRowSelectionInterval(row, row);
            }
        });
    }

    private static void shot(Window w, String name) throws Exception {
        Thread.sleep(700);
        SwingUtilities.invokeAndWait(() -> { });
        Rectangle r = w.getBounds();
        ImageIO.write(robot.createScreenCapture(r), "png", new File(dir, name + ".png"));
        System.out.println("saved " + name);
    }

    private static AbstractButton button(Container c, String text) {
        for (AbstractButton b : find(c, AbstractButton.class)) {
            if (text.equals(b.getText())) {
                return b;
            }
        }
        throw new IllegalArgumentException("Нет кнопки " + text);
    }

    private static List<JTextField> texts(Object c) {
        List<JTextField> out = new ArrayList<>();
        for (JTextField t : find((Container) c, JTextField.class)) {
            if (!(t.getParent() instanceof JComboBox) && t.isShowing() || t.getParent() == null || t.isEditable()) {
                if (!(SwingUtilities.getAncestorOfClass(JComboBox.class, t) != null)) {
                    out.add(t);
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> find(Container root, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                out.add((T) c);
            }
            if (c instanceof Container) {
                out.addAll(find((Container) c, type));
            }
        }
        if (root instanceof JFrame) {
            return find(((JFrame) root).getContentPane(), type).isEmpty() ? out : out;
        }
        return out;
    }

    interface Action {
        void run() throws Exception;
    }

    interface Supplier<T> {
        T get() throws Exception;
    }

    private static void on(Action a) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                a.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(300);
    }

    private static <T> T edt(Supplier<T> s) throws Exception {
        AtomicReference<T> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                ref.set(s.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return ref.get();
    }
}
