package by.bsuir.insurance.client;

import by.bsuir.insurance.client.net.ServerConnection;
import by.bsuir.insurance.client.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Точка входа клиентского приложения. Адрес сервера берётся из client.properties
 * (ресурс или файл в рабочем каталоге) и аргументов --server.host=..., --server.port=...
 */
public final class ClientApp {

    private ClientApp() {
    }

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (InputStream in = ClientApp.class.getResourceAsStream("/client.properties")) {
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        Path file = Paths.get("client.properties");
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        for (String a : args) {
            if (a.startsWith("--") && a.contains("=")) {
                p.setProperty(a.substring(2, a.indexOf('=')), a.substring(a.indexOf('=') + 1));
            }
        }
        ServerConnection.configure(p.getProperty("server.host", "localhost"),
                Integer.parseInt(p.getProperty("server.port", "5555")));
        SwingUtilities.invokeLater(() -> {
            setupLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }

    /** Стандартная тема Nimbus и увеличенный шрифт. */
    public static void setupLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // остаётся тема по умолчанию
        }
        UIManager.put("defaultFont", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        UIManager.put("nimbusBase", new java.awt.Color(0x1F, 0x4E, 0x79));
        UIManager.put("nimbusFocus", new java.awt.Color(0x6A, 0x9B, 0xC8));
    }
}
