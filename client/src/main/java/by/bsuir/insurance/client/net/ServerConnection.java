package by.bsuir.insurance.client.net;

import by.bsuir.insurance.common.protocol.Request;
import by.bsuir.insurance.common.protocol.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Соединение клиентского приложения с сервером (одно на приложение). */
public final class ServerConnection {
    private static ServerConnection instance;

    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private ServerConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static synchronized void configure(String host, int port) {
        instance = new ServerConnection(host, port);
    }

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Соединение не настроено");
        }
        return instance;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    private void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setSoTimeout(30000);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Отправить запрос и дождаться ответа. При обрыве соединения выполняется одна повторная попытка. */
    public synchronized Response send(Request request) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                if (socket == null || socket.isClosed()) {
                    connect();
                }
                out.writeObject(request);
                out.flush();
                out.reset();
                return (Response) in.readObject();
            } catch (IOException e) {
                close();
                if (attempt > 0) {
                    throw new IOException("Нет связи с сервером " + getAddress(), e);
                }
            } catch (ClassNotFoundException e) {
                throw new IOException("Несовместимая версия сервера", e);
            }
        }
    }

    public synchronized void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // соединение уже закрыто
            }
        }
        socket = null;
    }
}
