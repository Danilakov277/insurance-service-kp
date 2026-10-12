package by.bsuir.insurance.server.net;

import by.bsuir.insurance.common.exception.InsuranceException;
import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Обслуживание одного подключённого клиента в отдельном потоке пула:
 * чтение запросов, выполнение команд, отправка ответов.
 */
public class ClientHandler implements Runnable {
    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final CommandDispatcher dispatcher;
    private final Session session;

    public ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.session = new Session(socket.getRemoteSocketAddress().toString());
    }

    @Override
    public void run() {
        LOG.info("Подключение клиента " + session.getRemoteAddress());
        try (Socket s = socket;
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.flush();
            while (!Thread.currentThread().isInterrupted()) {
                Object obj = in.readObject();
                if (!(obj instanceof Request)) {
                    send(out, Response.error("Некорректный запрос"));
                    continue;
                }
                send(out, process((Request) obj));
            }
        } catch (EOFException | SocketException e) {
            LOG.info("Клиент " + session.getRemoteAddress() + " отключился");
        } catch (IOException | ClassNotFoundException e) {
            LOG.log(Level.WARNING, "Ошибка обмена с клиентом " + session.getRemoteAddress(), e);
        }
    }

    private Response process(Request request) {
        String who = session.isAuthenticated() ? session.getUser().getLogin() : "гость";
        try {
            Response r = dispatcher.dispatch(session, request);
            LOG.fine(who + ": " + request.getCommand() + " — OK");
            return r;
        } catch (InsuranceException e) {
            LOG.info(who + ": " + request.getCommand() + " — " + e.getMessage());
            return Response.error(e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, who + ": ошибка БД при выполнении " + request.getCommand(), e);
            return Response.error("Ошибка базы данных. Повторите операцию позже");
        } catch (IllegalArgumentException | ClassCastException | NullPointerException e) {
            LOG.log(Level.WARNING, who + ": некорректные параметры " + request, e);
            return Response.error("Некорректные параметры запроса: " + e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, who + ": внутренняя ошибка при выполнении " + request.getCommand(), e);
            return Response.error("Внутренняя ошибка сервера");
        }
    }

    private void send(ObjectOutputStream out, Response response) throws IOException {
        out.writeObject(response);
        out.flush();
        out.reset();
    }
}
