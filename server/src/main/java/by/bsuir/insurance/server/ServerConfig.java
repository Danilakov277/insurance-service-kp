package by.bsuir.insurance.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Настройки сервера (паттерн «Одиночка»). Порядок: значения по умолчанию из ресурса
 * server.properties → файл server.properties в рабочем каталоге (или указанный --config=...)
 * → аргументы командной строки вида --ключ=значение.
 */
public final class ServerConfig {
    private static ServerConfig instance;
    private final Properties props = new Properties();

    private ServerConfig() {
    }

    public static synchronized ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }

    public synchronized void load(String[] args) throws IOException {
        try (InputStream in = ServerConfig.class.getResourceAsStream("/server.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        }
        String file = "server.properties";
        for (String a : args) {
            if (a.startsWith("--config=")) {
                file = a.substring("--config=".length());
            }
        }
        Path path = Paths.get(file);
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                props.load(r);
            }
        }
        for (String a : args) {
            if (a.startsWith("--") && a.contains("=") && !a.startsWith("--config=")) {
                int eq = a.indexOf('=');
                props.setProperty(a.substring(2, eq), a.substring(eq + 1));
            }
        }
    }

    public String get(String key) {
        return props.getProperty(key);
    }

    public int getInt(String key, int def) {
        String v = props.getProperty(key);
        return v == null ? def : Integer.parseInt(v.trim());
    }

    public int getPort() { return getInt("server.port", 5555); }
    public int getThreads() { return getInt("server.threads", 20); }
    public String getDbUrl() { return get("db.url"); }
    public String getDbUser() { return get("db.user"); }
    public String getDbPassword() { return props.getProperty("db.password", ""); }
    public int getDbPoolSize() { return getInt("db.pool.size", 10); }
}
