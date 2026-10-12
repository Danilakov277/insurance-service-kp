package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.Role;
import by.bsuir.insurance.common.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Доступ к учётным записям. */
public class UserDao extends AbstractDao<User> {
    private static final String SELECT =
            "SELECT u.id, u.login, u.password_hash, u.salt, r.name AS role, u.is_blocked, u.created_at, "
            + "coalesce(c.id, a.id, 0) AS person_id, "
            + "coalesce(c.last_name || ' ' || c.first_name, a.last_name || ' ' || a.first_name, u.login) AS display_name "
            + "FROM users u JOIN roles r ON r.id = u.role_id "
            + "LEFT JOIN clients c ON c.user_id = u.id LEFT JOIN agents a ON a.user_id = u.id ";

    /** Учётная запись вместе с хешем пароля (используется только на сервере). */
    public static class Credentials {
        public final User user;
        public final String hash;
        public final String salt;

        Credentials(User user, String hash, String salt) {
            this.user = user;
            this.hash = hash;
            this.salt = salt;
        }
    }

    @Override
    protected User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setLogin(rs.getString("login"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setBlocked(rs.getBoolean("is_blocked"));
        u.setCreatedAt(dateTime(rs, "created_at"));
        u.setPersonId(rs.getLong("person_id"));
        u.setDisplayName(rs.getString("display_name"));
        return u;
    }

    public Credentials findCredentials(Connection c, String login) throws SQLException {
        try (java.sql.PreparedStatement ps = c.prepareStatement(SELECT + "WHERE lower(u.login) = lower(?)")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Credentials(map(rs), rs.getString("password_hash"), rs.getString("salt"));
            }
        }
    }

    public boolean loginExists(Connection c, String login) throws SQLException {
        return scalarLong(c, "SELECT count(*) FROM users WHERE lower(login) = lower(?)", login) > 0;
    }

    public long insert(Connection c, String login, String hash, String salt, Role role) throws SQLException {
        return insert(c, "INSERT INTO users (login, password_hash, salt, role_id) "
                + "VALUES (?, ?, ?, (SELECT id FROM roles WHERE name = ?))", login, hash, salt, role);
    }

    public List<User> listAll(Connection c) throws SQLException {
        return list(c, SELECT + "ORDER BY r.name, u.login");
    }

    public User findById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE u.id = ?", id);
    }

    public void setBlocked(Connection c, long userId, boolean blocked) throws SQLException {
        update(c, "UPDATE users SET is_blocked = ? WHERE id = ?", blocked, userId);
    }
}
