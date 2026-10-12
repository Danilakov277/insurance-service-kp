package by.bsuir.insurance.client.ui;

import by.bsuir.insurance.client.net.ServerConnection;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Общие функции пользовательского интерфейса. */
public final class Ui {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public static final Color ACCENT = new Color(0x1F, 0x4E, 0x79);
    public static final Color ACCENT_LIGHT = new Color(0xE8, 0xF0, 0xF8);
    public static final Color OK = new Color(0x2E, 0x7D, 0x32);
    public static final Color WARN = new Color(0xC6, 0x28, 0x28);

    private Ui() {
    }

    /**
     * Выполнить запрос к серверу. При ошибке показывает сообщение и возвращает null.
     */
    public static <T> T call(Component parent, Request request) {
        Response r = callRaw(parent, request);
        if (r == null) {
            return null;
        }
        return r.getData();
    }

    /** Выполнить запрос и показать сообщение сервера при успехе. */
    public static <T> T callWithMessage(Component parent, Request request) {
        Response r = callRaw(parent, request);
        if (r == null) {
            return null;
        }
        info(parent, r.getMessage());
        return r.getData();
    }

    private static Response callRaw(Component parent, Request request) {
        Cursor old = parent == null ? null : parent.getCursor();
        if (parent != null) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        try {
            Response r = ServerConnection.getInstance().send(request);
            if (!r.isSuccess()) {
                error(parent, r.getMessage());
                return null;
            }
            return r;
        } catch (IOException e) {
            error(parent, e.getMessage());
            return null;
        } finally {
            if (parent != null) {
                parent.setCursor(old);
            }
        }
    }

    public static Request req(CommandType type) {
        return new Request(type);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Сообщение", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Подтверждение",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static String format(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static String format(LocalDateTime d) {
        return d == null ? "" : DATE_TIME.format(d);
    }

    /** Разбор даты в формате дд.мм.гггг; при ошибке — исключение с понятным текстом. */
    public static LocalDate parseDate(String text, String field) {
        try {
            return LocalDate.parse(text.trim(), DATE);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Поле «" + field + "»: дата в формате дд.мм.гггг");
        }
    }

    public static BigDecimal parseMoney(String text, String field) {
        try {
            BigDecimal v = new BigDecimal(text.trim().replace(" ", "").replace(',', '.'));
            if (v.signum() <= 0) {
                throw new NumberFormatException();
            }
            return v;
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Поле «" + field + "»: введите положительное число");
        }
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 16f));
        l.setForeground(ACCENT);
        return l;
    }

    public static JLabel hint(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        l.setForeground(Color.DARK_GRAY);
        return l;
    }

    public static JLabel center(String text) {
        return new JLabel(text, SwingConstants.CENTER);
    }

    public static Border padding(int p) {
        return BorderFactory.createEmptyBorder(p, p, p, p);
    }

    public static <T extends JComponent> T titled(T c, String title) {
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), padding(6)));
        return c;
    }
}
