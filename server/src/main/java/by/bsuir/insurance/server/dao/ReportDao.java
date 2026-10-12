package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.LossRatioRow;
import by.bsuir.insurance.common.model.SalesReportRow;
import by.bsuir.insurance.common.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Аналитические запросы для отчётов администратора. */
public class ReportDao {

    /** Продажи агентов: договоры, оформленные в период, сумма премий и вознаграждение. */
    public List<SalesReportRow> sales(Connection c, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT a.last_name || ' ' || left(a.first_name, 1) || '.' || coalesce(left(a.patronymic, 1) || '.', '') "
                + "AS name, a.commission_rate, count(p.id) AS cnt, coalesce(sum(p.premium), 0) AS prem "
                + "FROM agents a LEFT JOIN policies p ON p.agent_id = a.id AND p.created_at::date BETWEEN ? AND ? "
                + "GROUP BY a.id ORDER BY prem DESC, name";
        List<SalesReportRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.sql.Date.valueOf(from));
            ps.setObject(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal prem = rs.getBigDecimal("prem");
                    rows.add(new SalesReportRow(rs.getString("name"), rs.getInt("cnt"), Money.round(prem),
                            Money.round(prem.multiply(rs.getBigDecimal("commission_rate")))));
                }
            }
        }
        return rows;
    }

    /**
     * Убыточность по видам страхования: собранные за период взносы, выплаты по решениям за период,
     * коэффициент убыточности = выплаты / премии × 100 %.
     */
    public List<LossRatioRow> lossRatio(Connection c, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT t.name, "
                + "(SELECT coalesce(sum(pm.amount), 0) FROM payments pm JOIN policies p ON p.id = pm.policy_id "
                + "  WHERE p.type_id = t.id AND pm.paid_date BETWEEN ? AND ?) AS prem, "
                + "(SELECT coalesce(sum(cl.payout_amount), 0) FROM claims cl JOIN policies p ON p.id = cl.policy_id "
                + "  WHERE p.type_id = t.id AND cl.status = 'APPROVED' AND cl.decided_at::date BETWEEN ? AND ?) AS pay "
                + "FROM insurance_types t ORDER BY t.id";
        List<LossRatioRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            java.sql.Date f = java.sql.Date.valueOf(from);
            java.sql.Date t = java.sql.Date.valueOf(to);
            ps.setDate(1, f);
            ps.setDate(2, t);
            ps.setDate(3, f);
            ps.setDate(4, t);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal prem = rs.getBigDecimal("prem");
                    BigDecimal pay = rs.getBigDecimal("pay");
                    BigDecimal ratio = prem.signum() == 0 ? null
                            : pay.multiply(Money.HUNDRED).divide(prem, 1, RoundingMode.HALF_UP);
                    if (prem.signum() == 0 && pay.signum() == 0) {
                        continue;
                    }
                    rows.add(new LossRatioRow(rs.getString("name"), Money.round(prem), Money.round(pay), ratio));
                }
            }
        }
        return rows;
    }
}
