package by.bsuir.insurance.client;

import by.bsuir.insurance.client.net.ServerConnection;
import by.bsuir.insurance.client.ui.LoginFrame;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.admin.AdminFrame;
import by.bsuir.insurance.client.ui.agent.AgentFrame;
import by.bsuir.insurance.client.ui.client.ClientFrame;
import by.bsuir.insurance.common.model.PolicyStatus;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Сквозной пример работы с клиентским приложением со снимками экрана для раздела 4
 * пояснительной записки: проходит все варианты использования, включая диалоги.
 * Запуск при работающем сервере и базе с демонстрационными данными:
 * java -cp ... by.bsuir.insurance.client.ScreenshotTour [папка_для_снимков]
 */
public final class ScreenshotTour {
    private static final int X = 30;
    private static final int Y = 30;
    private static File dir;
    private static Robot robot;

    private ScreenshotTour() {
    }

    public static void main(String[] args) {
        try {
            run(args);
            System.exit(0);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        robot = new Robot();
        ServerConnection.configure("localhost", 5555);
        SwingUtilities.invokeAndWait(ClientApp::setupLookAndFeel);
        String today = Ui.format(LocalDate.now());

        // ---- UC2: ошибка входа
        LoginFrame login = edt(() -> place(new LoginFrame()));
        on(() -> {
            texts(login).get(0).setText("ivanov");
            texts(login).get(1).setText("client12");
        });
        shot(login, "01_login");
        Window err = click(button(login, "Войти"));
        shot(login, err, "02_login_error");
        answer(err, "ОК");

        // ---- UC1: регистрация клиента
        Window reg = click(button(login, "Регистрация клиента"));
        on(() -> {
            List<JTextField> t = texts((Container) reg);
            String[] v = {"petrova", "secret77", "secret77", "Петрова", "Алина", "Сергеевна", "21.06.1999",
                "MP4567812", "+375 29 765-43-21", "г. Минск, ул. Кирова, 12-5"};
            for (int i = 0; i < v.length; i++) {
                t.get(i).setText(v[i]);
            }
        });
        shot(login, reg, "03_register");
        fire(button((Container) reg, "Зарегистрироваться"));
        JFrame client = waitFrame(ClientFrame.class);
        place(client);

        // ---- UC3: расчёт стоимости, ошибка ввода, UC4: заявка
        on(() -> {
            List<JComboBox> b = find(client, JComboBox.class);
            b.get(0).setSelectedIndex(0);
            texts(client).get(0).setText("80000");
            b.get(1).setSelectedItem(12);
            b.get(2).setSelectedItem(1);
            b.get(3).setSelectedItem(4);
            find(client, JTextArea.class).get(0).setText(
                    "Двухкомнатная квартира 54 м², кирпичный дом 2008 г., г. Минск, ул. Кирова, 12-5");
            texts(client).get(0).setText("1000");
        });
        err = click(button(client, "Рассчитать стоимость"));
        shot(client, err, "04_calc_error");
        answer(err, "ОК");
        on(() -> {
            texts(client).get(0).setText("80000");
            button(client, "Рассчитать стоимость").doClick();
        });
        shot(client, "05_calc");
        Window msg = click(button(client, "Подать заявку"));
        shot(client, msg, "06_submit");
        answer(msg, "ОК");
        tab(client, 1);
        shot(client, "07_my_applications");
        close(client);

        // ---- UC5: рассмотрение заявок агентом
        JFrame agent = open("kovaleva", "agent123");
        selectRow(agent, 0, "Петрова");
        shot(agent, "08_agent_applications");
        msg = click(button(agent, "Одобрить и оформить полис"));
        shot(agent, msg, "09_policy_issued");
        answer(msg, "ОК");
        selectRow(agent, 0, "Романов");
        Window input = click(button(agent, "Отклонить"));
        on(() -> texts((Container) input).get(0).setText("Не представлена справка о состоянии здоровья"));
        shot(agent, input, "10_reject");
        answer(input, "ОК");
        answer(waitDialog(), "ОК");
        close(agent);

        // ---- UC12, UC6: договор клиента и оплата взноса
        JFrame client2 = open("petrova", "secret77");
        tab(client2, 2);
        selectRow(client2, 0, "INS");
        shot(client2, "11_my_policy");
        Window confirm = click(button(client2, "Внести очередной взнос"));
        shot(client2, confirm, "12_pay_confirm");
        answer(confirm, "Да");
        answer(waitDialog(), "ОК");
        shot(client2, "13_after_payment");
        close(client2);

        // ---- UC7: заявление о страховом случае
        JFrame ivanov = open("ivanov", "client123");
        tab(ivanov, 3);
        on(() -> {
            Container p = selectedTab(ivanov);
            JComboBox<?> policy = find(p, JComboBox.class).get(0);
            for (int i = 0; i < policy.getItemCount(); i++) {
                if (String.valueOf(policy.getItemAt(i)).contains("000051")) {
                    policy.setSelectedIndex(i);
                }
            }
            List<JTextField> t = texts(p);
            t.get(0).setText(Ui.format(LocalDate.now().minusDays(2)));
            t.get(1).setText("640");
            find(p, JTextArea.class).get(0).setText(
                    "Обращение в клинику (Турция, Анталья): острый отит, амбулаторное лечение и лекарства");
        });
        shot(ivanov, "14_claim_form");
        msg = click(button(ivanov, "Заявить о страховом случае"));
        shot(ivanov, msg, "15_claim_registered");
        answer(msg, "ОК");
        shot(ivanov, "16_my_claims");
        close(ivanov);

        // ---- UC8: урегулирование страхового случая
        JFrame agent2 = open("kovaleva", "agent123");
        tab(agent2, 2);
        selectRow(agent2, 0, "отит");
        JFrame a = agent2;
        on(() -> {
            List<JTextField> t = texts(selectedTab(a));
            t.get(0).setText("600");
            t.get(1).setText("Счёт клиники проверен, расходы на такси исключены");
            button(a, "Рассчитать выплату").doClick();
        });
        shot(agent2, "17_payout_calc");
        confirm = click(button(agent2, "Принять решение"));
        shot(agent2, confirm, "18_payout_confirm");
        answer(confirm, "Да");
        answer(waitDialog(), "ОК");

        // ---- UC9: продление, UC10: расторжение
        tab(agent2, 1);
        on(() -> find(selectedTab(a), JComboBox.class).get(0).setSelectedItem(PolicyStatus.EXPIRED));
        selectRow(agent2, 0, "000015");
        shot(agent2, "19_policies_expired");
        confirm = click(button(agent2, "Продлить"));
        answer(confirm, "Да");
        msg = waitDialog();
        shot(agent2, msg, "20_renewed");
        answer(msg, "ОК");
        on(() -> find(selectedTab(a), JComboBox.class).get(0).setSelectedItem(PolicyStatus.ACTIVE));
        selectRow(agent2, 0, "000047");
        confirm = click(button(agent2, "Расторгнуть досрочно"));
        shot(agent2, confirm, "21_terminate");
        answer(confirm, "Да");
        answer(waitDialog(), "ОК");

        // ---- UC11: контроль просрочек и истекающих договоров
        tab(agent2, 3);
        shot(agent2, "22_control");
        msg = click(button(agent2, "Пересчитать состояния договоров"));
        answer(msg, "ОК");
        close(agent2);

        // ---- UC13: виды страхования и тарифы
        JFrame admin = open("admin", "admin123");
        selectRow(admin, 0, "садового");
        on(() -> texts(selectedTab(admin)).get(2).setText("0,0095"));
        shot(admin, "23_types");
        msg = click(button(admin, "Сохранить"));
        answer(msg, "ОК");

        // ---- UC14: учётные записи агентов и пользователей
        tab(admin, 1);
        on(() -> {
            List<JTextField> t = texts(selectedTab(admin));
            String[] v = {"lazarenko", "agent123", "Лазаренко", "Ольга", "Викторовна", "Страховой агент", "8"};
            for (int i = 0; i < v.length; i++) {
                t.get(i).setText(v[i]);
            }
        });
        shot(admin, "24_new_agent");
        msg = click(button(admin, "Создать учётную запись агента"));
        answer(msg, "ОК");
        tab(admin, 2);
        selectRow(admin, 0, "belov");
        msg = click(button(admin, "Заблокировать"));
        shot(admin, msg, "25_blocked");
        answer(msg, "ОК");

        // ---- UC15, UC17: отчёт о продажах и выгрузка
        tab(admin, 3);
        on(() -> {
            List<JTextField> t = texts(selectedTab(admin));
            t.get(0).setText("01.01.2026");
            t.get(1).setText(today);
            button(selectedTab(admin), "Сформировать").doClick();
        });
        shot(admin, "26_sales");
        Window chooser = click(button(selectedTab(admin), "Выгрузить в файл"));
        File out = new File(dir, "sales_report.txt");
        // пустая папка вместо «Документов» пользователя, чтобы на снимок не попали чужие файлы
        File exportDir = new File(dir, "Отчёты");
        exportDir.mkdirs();
        on(() -> {
            JFileChooser fc = find((Container) chooser, JFileChooser.class).get(0);
            fc.setCurrentDirectory(exportDir);
            fc.setSelectedFile(new File(exportDir, "sales_report.txt"));
        });
        shot(admin, chooser, "27_export");
        on(() -> {
            JFileChooser fc = find((Container) chooser, JFileChooser.class).get(0);
            fc.setSelectedFile(out);
            fc.approveSelection();
        });
        answer(waitDialog(), "ОК");

        // ---- UC16: анализ убыточности
        tab(admin, 4);
        on(() -> {
            List<JTextField> t = texts(selectedTab(admin));
            t.get(0).setText("01.01.2025");
            t.get(1).setText(today);
            button(selectedTab(admin), "Сформировать").doClick();
        });
        shot(admin, "28_loss_ratio");
        close(admin);

        // ---- вход заблокированного пользователя
        LoginFrame blocked = edt(() -> place(new LoginFrame()));
        on(() -> {
            texts(blocked).get(0).setText("belov");
            texts(blocked).get(1).setText("client123");
        });
        err = click(button(blocked, "Войти"));
        shot(blocked, err, "29_blocked_login");
        answer(err, "ОК");
        System.out.println("Снимков сохранено в " + dir.getAbsolutePath());
    }

    // ------------------------------------------------------------------ окна

    private static <T extends Window> T place(T w) {
        w.setLocation(X, Y);
        if (w instanceof JFrame) {
            ((JFrame) w).setAlwaysOnTop(true);
            w.setVisible(true);
            w.toFront();
        }
        return w;
    }

    private static JFrame open(String login, String password) throws Exception {
        User u = edt(() -> Ui.call(null, Ui.req(CommandType.LOGIN).with("login", login).with("password", password)));
        if (u == null) {
            throw new IllegalStateException("Не удалось войти: " + login);
        }
        JFrame f = edt(() -> {
            JFrame fr;
            switch (u.getRole()) {
                case CLIENT: fr = new ClientFrame(u); break;
                case AGENT: fr = new AgentFrame(u); break;
                default: fr = new AdminFrame(u); break;
            }
            return place(fr);
        });
        Thread.sleep(900);
        return f;
    }

    private static JFrame waitFrame(Class<? extends JFrame> type) throws Exception {
        for (int i = 0; i < 100; i++) {
            for (Window w : Window.getWindows()) {
                if (type.isInstance(w) && w.isShowing()) {
                    Thread.sleep(900);
                    return (JFrame) w;
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Не открылось окно " + type.getSimpleName());
    }

    private static void close(Window w) throws Exception {
        on(w::dispose);
    }

    private static void tab(JFrame f, int index) throws Exception {
        on(() -> find(f, JTabbedPane.class).get(0).setSelectedIndex(index));
        Thread.sleep(500);
    }

    private static Container selectedTab(JFrame f) {
        return (Container) find(f, JTabbedPane.class).get(0).getSelectedComponent();
    }

    /** Выбрать в таблице текущей вкладки строку, содержащую текст. */
    private static void selectRow(JFrame f, int tableIndex, String text) throws Exception {
        on(() -> {
            JTable t = find(selectedTab(f), JTable.class).get(tableIndex);
            for (int r = 0; r < t.getRowCount(); r++) {
                for (int c = 0; c < t.getColumnCount(); c++) {
                    if (String.valueOf(t.getValueAt(r, c)).contains(text)) {
                        t.setRowSelectionInterval(r, r);
                        t.scrollRectToVisible(t.getCellRect(r, 0, true));
                        return;
                    }
                }
            }
            throw new IllegalStateException("Нет строки с текстом «" + text + "»");
        });
    }

    // ------------------------------------------------------------------ диалоги

    /** Нажать кнопку, открывающую модальный диалог, и дождаться его появления. */
    private static Window click(AbstractButton b) throws Exception {
        fire(b);
        return waitDialog();
    }

    private static void fire(AbstractButton b) {
        SwingUtilities.invokeLater(b::doClick);
    }

    private static Window waitDialog() throws Exception {
        for (int i = 0; i < 150; i++) {
            for (Window w : Window.getWindows()) {
                if (w instanceof Dialog && w.isShowing()) {
                    Thread.sleep(700);
                    return w;
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Диалог не появился");
    }

    /** Нажать кнопку диалога с указанной надписью и дождаться его закрытия. */
    private static void answer(Window dialog, String buttonText) throws Exception {
        AbstractButton b = edt(() -> button((Container) dialog, buttonText));
        fire(b);
        for (int i = 0; i < 100 && dialog.isShowing(); i++) {
            Thread.sleep(100);
        }
        Thread.sleep(400);
    }

    // ------------------------------------------------------------------ снимки

    private static void shot(Window w, String name) throws Exception {
        shot(w, null, name);
    }

    /**
     * Снимок окна и диалога поверх него. Каждое окно снимается отдельно и переносится
     * на нейтральный фон: в снимок не попадает ничего, кроме окон программы.
     */
    private static void shot(Window w, Window dialog, String name) throws Exception {
        Thread.sleep(600);
        SwingUtilities.invokeAndWait(() -> { });
        Rectangle fr = visible(w);
        Rectangle all = dialog == null ? fr : fr.union(visible(dialog));
        BufferedImage frame = capture(fr);
        double scale = frame.getWidth() / (double) fr.width;
        BufferedImage img = new BufferedImage((int) Math.round(all.width * scale),
                (int) Math.round(all.height * scale), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(0xF2, 0xF2, 0xF2));
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(frame, (int) Math.round((fr.x - all.x) * scale), (int) Math.round((fr.y - all.y) * scale), null);
        if (dialog != null) {
            Rectangle dr = visible(dialog);
            g.drawImage(capture(dr), (int) Math.round((dr.x - all.x) * scale), (int) Math.round((dr.y - all.y) * scale), null);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(dir, name + ".png"));
        System.out.println("снимок " + name);
    }

    /**
     * Видимая часть окна. В Windows 10/11 границы окна включают невидимую рамку
     * изменения размера (она совпадает с левым отступом окна, около 7 пикселей слева, справа
     * и снизу) — её отрезаем, чтобы в снимок не попадал рабочий стол.
     */
    private static Rectangle visible(Window w) {
        Rectangle r = w.getBounds();
        if (System.getProperty("os.name", "").startsWith("Windows")) {
            int b = Math.max(0, w.getInsets().left);
            r = new Rectangle(r.x + b, r.y, r.width - 2 * b, r.height - b);
        }
        return r;
    }

    /** На Java 9+ берётся снимок в полном разрешении экрана (масштаб Windows 125–150 %). */
    private static BufferedImage capture(Rectangle r) {
        try {
            Method m = Robot.class.getMethod("createMultiResolutionScreenCapture", Rectangle.class);
            Object mr = m.invoke(robot, r);
            Method variants = Class.forName("java.awt.image.MultiResolutionImage").getMethod("getResolutionVariants");
            List<?> list = (List<?>) variants.invoke(mr);
            Image best = (Image) list.get(list.size() - 1);
            BufferedImage img = new BufferedImage(best.getWidth(null), best.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.drawImage(best, 0, 0, null);
            g.dispose();
            return img;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return robot.createScreenCapture(r);
        }
    }

    // ------------------------------------------------------------------ поиск компонентов

    private static AbstractButton button(Container c, String text) {
        for (AbstractButton b : find(c, AbstractButton.class)) {
            if (text.equals(b.getText()) && b.isShowing()) {
                return b;
            }
        }
        throw new IllegalArgumentException("Нет кнопки «" + text + "»");
    }

    /** Видимые редактируемые текстовые поля (без полей внутри выпадающих списков). */
    private static List<JTextField> texts(Container c) {
        List<JTextField> out = new ArrayList<>();
        for (JTextField t : find(c, JTextField.class)) {
            if (t.isShowing() && t.isEditable() && SwingUtilities.getAncestorOfClass(JComboBox.class, t) == null) {
                out.add(t);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> find(Container root, Class<T> type) {
        List<T> out = new ArrayList<>();
        Container start = root instanceof JFrame ? ((JFrame) root).getContentPane()
                : root instanceof JDialog ? ((JDialog) root).getContentPane() : root;
        collect(start, type, out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static <T> void collect(Container c, Class<T> type, List<T> out) {
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) {
                out.add((T) child);
            }
            if (child instanceof Container) {
                collect((Container) child, type, out);
            }
        }
    }

    // ------------------------------------------------------------------ поток EDT

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
        Thread.sleep(400);
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
