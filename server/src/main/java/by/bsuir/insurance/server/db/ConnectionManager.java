package by.bsuir.insurance.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Пул соединений с СУБД (паттерн «Одиночка»). Потоки сервера берут соединение из пула
 * и возвращают его после выполнения запроса.
 */
public final class ConnectionManager {
    private static ConnectionManager instance;

    private final BlockingQueue<Connection> pool;
    private final String url;
    private final String user;
    private final String password;

    private ConnectionManager(String url, String user, String password, int size) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.pool = new ArrayBlockingQueue<>(size);
    }

    public static synchronized void init(String url, String user, String password, int size) throws SQLException {
        instance = new ConnectionManager(url, user, password, size);
        for (int i = 0; i < size; i++) {
            instance.pool.add(instance.open());
        }
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Пул соединений не инициализирован");
        }
        return instance;
    }

    private Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Получить соединение (ожидание не дольше 10 с). */
    public Connection acquire() throws SQLException {
        try {
            Connection c = pool.poll(10, TimeUnit.SECONDS);
            if (c == null) {
                throw new SQLException("Нет свободных соединений с базой данных");
            }
            if (!c.isValid(2)) {
                c = open();
            }
            c.setAutoCommit(true);
            return c;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Ожидание соединения прервано", e);
        }
    }

    public void release(Connection c) {
        if (c != null) {
            pool.offer(c);
        }
    }

    /** Выполнить действие с соединением из пула. */
    public <T> T withConnection(SqlFunction<T> action) throws Exception {
        Connection c = acquire();
        try {
            return action.apply(c);
        } finally {
            release(c);
        }
    }

    /** Выполнить действие в транзакции: COMMIT при успехе, ROLLBACK при исключении. */
    public <T> T inTransaction(SqlFunction<T> action) throws Exception {
        Connection c = acquire();
        try {
            c.setAutoCommit(false);
            T result = action.apply(c);
            c.commit();
            return result;
        } catch (Exception e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
            release(c);
        }
    }

    public void shutdown() {
        Connection c;
        while ((c = pool.poll()) != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // соединение уже закрыто
            }
        }
    }

    /** Функция, работающая с соединением и способная бросать исключения. */
    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws Exception;
    }
}
