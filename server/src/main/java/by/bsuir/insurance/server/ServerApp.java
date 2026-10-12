package by.bsuir.insurance.server;

import by.bsuir.insurance.server.db.ConnectionManager;
import by.bsuir.insurance.server.net.ClientHandler;
import by.bsuir.insurance.server.net.CommandDispatcher;
import by.bsuir.insurance.server.service.PolicyService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Точка входа сервера. Принимает подключения и передаёт каждое в пул потоков;
 * периодически пересчитывает состояния договоров.
 *
 * Пример запуска: java -jar insurance-server.jar --server.port=5555 --db.password=secret
 */
public final class ServerApp {
    private static final Logger LOG = Logger.getLogger(ServerApp.class.getName());

    private ServerApp() {
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
        ServerConfig cfg = ServerConfig.getInstance();
        try {
            cfg.load(args);
            ConnectionManager.init(cfg.getDbUrl(), cfg.getDbUser(), cfg.getDbPassword(), cfg.getDbPoolSize());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Не удалось запустить сервер: " + e.getMessage(), e);
            System.exit(1);
            return;
        }
        LOG.info("Подключение к БД установлено: " + cfg.getDbUrl());

        PolicyService policyService = new PolicyService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                policyService.refreshStatuses();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Ошибка пересчёта состояний договоров", e);
            }
        }, 0, 6, TimeUnit.HOURS);

        ExecutorService pool = Executors.newFixedThreadPool(cfg.getThreads());
        CommandDispatcher dispatcher = new CommandDispatcher();
        try (ServerSocket serverSocket = new ServerSocket(cfg.getPort())) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Остановка сервера");
                scheduler.shutdownNow();
                pool.shutdownNow();
                ConnectionManager.getInstance().shutdown();
            }));
            LOG.info("Сервер запущен на порту " + cfg.getPort() + ", потоков: " + cfg.getThreads());
            while (!pool.isShutdown()) {
                Socket socket = serverSocket.accept();
                pool.execute(new ClientHandler(socket, dispatcher));
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Ошибка серверного сокета: " + e.getMessage(), e);
            System.exit(2);
        }
    }
}
