package by.bsuir.insurance.server.net;

import by.bsuir.insurance.common.model.User;

/** Сеанс работы клиента: создаётся на каждое подключение. */
public class Session {
    private final String remoteAddress;
    private User user;

    public Session(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getRemoteAddress() { return remoteAddress; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public boolean isAuthenticated() {
        return user != null;
    }

    /** Идентификатор клиента или агента, связанного с текущим пользователем. */
    public long personId() {
        return user.getPersonId();
    }
}
