package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.ControlItem;
import by.bsuir.insurance.common.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Доступ к графикам страховых взносов. */
public class PaymentDao extends AbstractDao<Payment> {

    @Override
    protected Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPolicyId(rs.getLong("policy_id"));
        p.setInstallmentNo(rs.getInt("installment_no"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setDueDate(date(rs, "due_date"));
        p.setPaidDate(date(rs, "paid_date"));
        return p;
    }

    public List<Payment> listByPolicy(Connection c, long policyId) throws SQLException {
        return list(c, "SELECT * FROM payments WHERE policy_id = ? ORDER BY installment_no", policyId);
    }

    /** Пакетная вставка графика взносов. */
    public void insertAll(Connection c, List<Payment> payments) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payments (policy_id, installment_no, amount, due_date) VALUES (?, ?, ?, ?)")) {
            for (Payment p : payments) {
                bind(ps, p.getPolicyId(), p.getInstallmentNo(), p.getAmount(), p.getDueDate());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void markPaid(Connection c, long policyId, int installmentNo, LocalDate date) throws SQLException {
        update(c, "UPDATE payments SET paid_date = ? WHERE policy_id = ? AND installment_no = ?",
                date, policyId, installmentNo);
    }

    /** Удалить неоплаченные взносы со сроком после даты (при расторжении). */
    public void deleteUnpaidAfter(Connection c, long policyId, LocalDate date) throws SQLException {
        update(c, "DELETE FROM payments WHERE policy_id = ? AND paid_date IS NULL AND due_date > ?", policyId, date);
    }

    /** Просроченные взносы по действующим и приостановленным договорам. */
    public List<ControlItem> listOverdue(Connection c, LocalDate today) throws SQLException {
        String sql = "SELECT p.id, p.policy_number, " + shortNameSql("c") + " AS client_name, c.phone, "
                + "pm.due_date, pm.amount FROM payments pm JOIN policies p ON p.id = pm.policy_id "
                + "JOIN clients c ON c.id = p.client_id "
                + "WHERE pm.paid_date IS NULL AND pm.due_date < ? AND p.status IN ('ACTIVE', 'SUSPENDED', 'WAITING_PAYMENT') "
                + "ORDER BY pm.due_date";
        List<ControlItem> items = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, today);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate due = date(rs, "due_date");
                    items.add(new ControlItem(ControlItem.Kind.OVERDUE_PAYMENT, rs.getLong("id"),
                            rs.getString("policy_number"), rs.getString("client_name"), rs.getString("phone"),
                            due, rs.getBigDecimal("amount"), ChronoUnit.DAYS.between(due, today)));
                }
            }
        }
        return items;
    }
}
