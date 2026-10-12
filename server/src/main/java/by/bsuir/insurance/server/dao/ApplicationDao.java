package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.ApplicationStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Доступ к заявкам на страхование. */
public class ApplicationDao extends AbstractDao<Application> {
    private static final String SELECT =
            "SELECT ap.*, " + shortNameSql("c") + " AS client_name, t.name AS type_name, "
            + shortNameSql("a") + " AS agent_name "
            + "FROM applications ap JOIN clients c ON c.id = ap.client_id "
            + "JOIN insurance_types t ON t.id = ap.type_id LEFT JOIN agents a ON a.id = ap.agent_id ";

    @Override
    protected Application map(ResultSet rs) throws SQLException {
        Application a = new Application();
        a.setId(rs.getLong("id"));
        a.setClientId(rs.getLong("client_id"));
        a.setClientName(rs.getString("client_name"));
        a.setTypeId(rs.getLong("type_id"));
        a.setTypeName(rs.getString("type_name"));
        a.setAgentId(longOrNull(rs, "agent_id"));
        a.setAgentName(rs.getString("agent_name"));
        a.setRequestedSum(rs.getBigDecimal("requested_sum"));
        a.setTermMonths(rs.getInt("term_months"));
        a.setFranchisePercent(rs.getInt("franchise_percent"));
        a.setInstallments(rs.getInt("installments"));
        a.setObjectDescription(rs.getString("object_description"));
        a.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        a.setDecisionComment(rs.getString("decision_comment"));
        a.setCreatedAt(dateTime(rs, "created_at"));
        a.setDecidedAt(dateTime(rs, "decided_at"));
        return a;
    }

    public Application findById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE ap.id = ?", id);
    }

    /** Заблокировать строку заявки до конца транзакции (защита от одновременного рассмотрения). */
    public Application lockById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE ap.id = ? FOR UPDATE OF ap", id);
    }

    public List<Application> listByClient(Connection c, long clientId) throws SQLException {
        return list(c, SELECT + "WHERE ap.client_id = ? ORDER BY ap.created_at DESC", clientId);
    }

    public List<Application> listByStatus(Connection c, ApplicationStatus status) throws SQLException {
        return list(c, SELECT + "WHERE ap.status = ? ORDER BY ap.created_at", status);
    }

    public long insert(Connection c, Application a) throws SQLException {
        return insert(c, "INSERT INTO applications (client_id, type_id, requested_sum, term_months, franchise_percent, "
                        + "installments, object_description) VALUES (?, ?, ?, ?, ?, ?, ?)",
                a.getClientId(), a.getTypeId(), a.getRequestedSum(), a.getTermMonths(), a.getFranchisePercent(),
                a.getInstallments(), a.getObjectDescription());
    }

    public void decide(Connection c, long id, ApplicationStatus status, long agentId, String comment)
            throws SQLException {
        update(c, "UPDATE applications SET status = ?, agent_id = ?, decision_comment = ?, decided_at = ? WHERE id = ?",
                status, agentId, comment, LocalDateTime.now(), id);
    }
}
