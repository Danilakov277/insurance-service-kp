package by.bsuir.insurance.common.protocol;

import java.io.Serializable;

/** Ответ сервера: признак успеха, сообщение и данные. */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Serializable data;

    private Response(boolean success, String message, Serializable data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static Response ok(Serializable data) {
        return new Response(true, "OK", data);
    }

    public static Response ok(String message, Serializable data) {
        return new Response(true, message, data);
    }

    public static Response error(String message) {
        return new Response(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }
}
