package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Доступ к договорам страхования. */
public class PolicyDao extends AbstractDao<Policy> {
    private static final String SELECT =
            "SELECT p.*, " + shortNameSql("c") + " AS client_name, " + shortNameSql("a") + " AS agent_name, "
            + "t.name AS type_name, "
            + "(SELECT coalesce(sum(amount), 0) FROM payments WHERE policy_id = p.id AND paid_date IS NOT NULL) AS paid, "
            + "(SELECT coalesce(sum(payout_amount), 0) FROM claims WHERE policy_id = p.id AND status = 'APPROVED') AS payouts "
            + "FROM policies p JOIN clients c ON c.id = p.client_id JOIN agents a ON a.id = p.agent_id "
            + "JOIN insurance_types t ON t.id = p.type_id ";

    @Override
    protected Policy map(ResultSet rs) throws SQLException {
        Policy p = new Policy();
        p.setId(rs.getLong("id"));
        p.setPolicyNumber(rs.getString("policy_number"));
        p.setApplicationId(longOrNull(rs, "application_id"));
        p.setClientId(rs.getLong("client_id"));
        p.setClientName(rs.getString("client_name"));
        p.setAgentId(rs.getLong("agent_id"));
        p.setAgentName(rs.getString("agent_name"));
        p.setTypeId(rs.getLong("type_id"));
        p.setTypeName(rs.getString("type_name"));
        p.setInsuredSum(rs.getBigDecimal("insured_sum"));
        p.setPremium(rs.getBigDecimal("premium"));
        p.setFranchise(rs.getBigDecimal("franchise"));
        p.setStartDate(date(rs, "start_date"));
        p.setEndDate(date(rs, "end_date"));
        p.setStatus(PolicyStatus.valueOf(rs.getString("status")));
        p.setCreatedAt(dateTime(rs, "created_at"));
        p.setTerminatedAt(dateTime(rs, "terminated_at"));
        p.setRefundAmount(rs.getBigDecimal("refund_amount"));
        p.setPaidAmount(rs.getBigDecimal("paid"));
        p.setPayoutsAmount(rs.getBigDecimal("payouts"));
        return p;
    }

    public Policy findById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE p.id = ?", id);
    }

    public Policy lockById(Connection c, long id) throws SQLException {
        return one(c, SELECT + "WHERE p.id = ? FOR UPDATE OF p", id);
    }

    public List<Policy> listByClient(Connection c, long clientId) throws SQLException {
        return list(c, SELECT + "WHERE p.client_id = ? ORDER BY p.start_date DESC", clientId);
    }

    /** Все договоры или договоры с указанным статусом. */
    public List<Policy> listAll(Connection c, PolicyStatus status) throws SQLException {
        if (status == null) {
            return list(c, SELECT + "ORDER BY p.created_at DESC");
        }
        return list(c, SELECT + "WHERE p.status = ? ORDER BY p.created_at DESC", status);
    }

    /** Договоры, состояние которых может измениться со временем. */
    public List<Policy> listChangeable(Connection c) throws SQLException {
        return list(c, SELECT + "WHERE p.status IN ('WAITING_PAYMENT', 'ACTIVE', 'SUSPENDED')");
    }

    /** Договоры, срок которых истекает в ближайшие days дней. */
    public List<Policy> listExpiring(Connection c, LocalDate today, int days) throws SQLException {
        return list(c, SELECT + "WHERE p.status IN ('ACTIVE', 'SUSPENDED') AND p.end_date BETWEEN ? AND ? "
                + "ORDER BY p.end_date", today, today.plusDays(days));
    }

    public long insert(Connection c, Policy p) throws SQLException {
        long id = insert(c, "INSERT INTO policies (policy_number, application_id, client_id, agent_id, type_id, "
                        + "insured_sum, premium, franchise, start_date, end_date, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "TMP-" + System.nanoTime(), p.getApplicationId(), p.getClientId(), p.getAgentId(), p.getTypeId(),
                p.getInsuredSum(), p.getPremium(), p.getFranchise(), p.getStartDate(), p.getEndDate(),
                PolicyStatus.WAITING_PAYMENT);
        String number = String.format("INS-%d-%06d", LocalDate.now().getYear(), id);
        update(c, "UPDATE policies SET policy_number = ? WHERE id = ?", number, id);
        p.setId(id);
        p.setPolicyNumber(number);
        return id;
    }

    public void updateStatus(Connection c, long id, PolicyStatus status) throws SQLException {
        if (status == PolicyStatus.TERMINATED) {
            update(c, "UPDATE policies SET status = ?, terminated_at = coalesce(terminated_at, ?), "
                    + "refund_amount = coalesce(refund_amount, 0) WHERE id = ?", status, LocalDateTime.now(), id);
        } else {
            update(c, "UPDATE policies SET status = ? WHERE id = ?", status, id);
        }
    }

    public void terminate(Connection c, long id, LocalDateTime at, BigDecimal refund) throws SQLException {
        update(c, "UPDATE policies SET status = 'TERMINATED', terminated_at = ?, refund_amount = ? WHERE id = ?",
                at, refund, id);
    }

    /** Есть ли уже договор-продление (следующий договор того же клиента и вида, начинающийся после окончания). */
    public boolean hasRenewal(Connection c, Policy p) throws SQLException {
        return scalarLong(c, "SELECT count(*) FROM policies WHERE client_id = ? AND type_id = ? AND start_date > ? "
                + "AND status <> 'TERMINATED'", p.getClientId(), p.getTypeId(), p.getEndDate().minusDays(1)) > 0;
    }
}
