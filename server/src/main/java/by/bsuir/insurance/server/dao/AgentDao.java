package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.Agent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Доступ к данным страховых агентов. */
public class AgentDao extends AbstractDao<Agent> {
    private static final String SELECT =
            "SELECT a.*, u.login, u.is_blocked FROM agents a JOIN users u ON u.id = a.user_id ";

    @Override
    protected Agent map(ResultSet rs) throws SQLException {
        Agent a = new Agent();
        a.setId(rs.getLong("id"));
        a.setUserId(rs.getLong("user_id"));
        a.setLastName(rs.getString("last_name"));
        a.setFirstName(rs.getString("first_name"));
        a.setPatronymic(rs.getString("patronymic"));
        a.setPosition(rs.getString("position"));
        a.setHireDate(date(rs, "hire_date"));
        a.setCommissionRate(rs.getBigDecimal("commission_rate"));
        a.setLogin(rs.getString("login"));
        a.setBlocked(rs.getBoolean("is_blocked"));
        return a;
    }

    public List<Agent> listAll(Connection c) throws SQLException {
        return list(c, SELECT + "ORDER BY a.last_name, a.first_name");
    }

    public Agent findById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE a.id = ?", id);
    }

    public long insert(Connection c, Agent a) throws SQLException {
        return insert(c, "INSERT INTO agents (user_id, last_name, first_name, patronymic, position, hire_date, "
                        + "commission_rate) VALUES (?, ?, ?, ?, ?, ?, ?)",
                a.getUserId(), a.getLastName(), a.getFirstName(), a.getPatronymic(), a.getPosition(),
                a.getHireDate(), a.getCommissionRate());
    }
}
