package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.ClaimStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Доступ к страховым случаям. */
public class ClaimDao extends AbstractDao<Claim> {
    private static final String SELECT =
            "SELECT cl.*, p.policy_number, t.name AS type_name, " + shortNameSql("c") + " AS client_name, "
            + shortNameSql("a") + " AS agent_name "
            + "FROM claims cl JOIN policies p ON p.id = cl.policy_id JOIN clients c ON c.id = p.client_id "
            + "JOIN insurance_types t ON t.id = p.type_id LEFT JOIN agents a ON a.id = cl.agent_id ";

    @Override
    protected Claim map(ResultSet rs) throws SQLException {
        Claim c = new Claim();
        c.setId(rs.getLong("id"));
        c.setPolicyId(rs.getLong("policy_id"));
        c.setPolicyNumber(rs.getString("policy_number"));
        c.setTypeName(rs.getString("type_name"));
        c.setClientName(rs.getString("client_name"));
        c.setIncidentDate(date(rs, "incident_date"));
        c.setReportDate(date(rs, "report_date"));
        c.setDescription(rs.getString("description"));
        c.setDamageAmount(rs.getBigDecimal("damage_amount"));
        c.setAssessedDamage(rs.getBigDecimal("assessed_damage"));
        c.setStatus(ClaimStatus.valueOf(rs.getString("status")));
        c.setPayoutAmount(rs.getBigDecimal("payout_amount"));
        c.setAgentId(longOrNull(rs, "agent_id"));
        c.setAgentName(rs.getString("agent_name"));
        c.setDecisionComment(rs.getString("decision_comment"));
        c.setDecidedAt(dateTime(rs, "decided_at"));
        return c;
    }

    public Claim findById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE cl.id = ?", id);
    }

    public Claim lockById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE cl.id = ? FOR UPDATE OF cl", id);
    }

    public List<Claim> listByClient(Connection c, long clientId) throws SQLException {
        return list(c, SELECT + "WHERE p.client_id = ? ORDER BY cl.report_date DESC", clientId);
    }

    public List<Claim> listByStatus(Connection c, ClaimStatus status) throws SQLException {
        return list(c, SELECT + "WHERE cl.status = ? ORDER BY cl.report_date", status);
    }

    public List<Claim> listAll(Connection c) throws SQLException {
        return list(c, SELECT + "ORDER BY cl.report_date DESC");
    }

    public long insert(Connection c, Claim cl) throws SQLException {
        return insert(c, "INSERT INTO claims (policy_id, incident_date, report_date, description, damage_amount) "
                        + "VALUES (?, ?, ?, ?, ?)",
                cl.getPolicyId(), cl.getIncidentDate(), cl.getReportDate(), cl.getDescription(), cl.getDamageAmount());
    }

    public void decide(Connection c, long id, ClaimStatus status, BigDecimal assessed, BigDecimal payout,
                       long agentId, String comment) throws SQLException {
        update(c, "UPDATE claims SET status = ?, assessed_damage = ?, payout_amount = ?, agent_id = ?, "
                + "decision_comment = ?, decided_at = ? WHERE id = ?",
                status, assessed, payout, agentId, comment, LocalDateTime.now(), id);
    }

    /** Сумма выплат по договору. */
    public BigDecimal sumPayouts(Connection c, long policyId) throws SQLException {
        return scalarDecimal(c, "SELECT coalesce(sum(payout_amount), 0) FROM claims "
                + "WHERE policy_id = ? AND status = 'APPROVED'", policyId);
    }

    /** Число выплат клиенту по событиям за период (для коэффициента бонус-малус). */
    public int countPaidClaims(Connection c, long clientId, LocalDate from, LocalDate to) throws SQLException {
        return (int) scalarLong(c, "SELECT count(*) FROM claims cl JOIN policies p ON p.id = cl.policy_id "
                + "WHERE p.client_id = ? AND cl.status = 'APPROVED' AND cl.payout_amount > 0 "
                + "AND cl.incident_date BETWEEN ? AND ?", clientId, from, to);
    }
}
