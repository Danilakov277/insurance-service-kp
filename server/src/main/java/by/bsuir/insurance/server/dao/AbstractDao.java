package by.bsuir.insurance.server.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс объектов доступа к данным (паттерн DAO). Содержит общие операции JDBC;
 * наследники задают SQL-запросы и отображение строки результата на объект.
 *
 * @param <T> тип сущности
 */
public abstract class AbstractDao<T> {

    /** Преобразование текущей строки результата в объект. */
    protected abstract T map(ResultSet rs) throws SQLException;

    protected List<T> list(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        }
    }

    protected T one(Connection c, String sql, Object... params) throws SQLException {
        List<T> l = list(c, sql, params);
        return l.isEmpty() ? null : l.get(0);
    }

    protected int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** INSERT ... RETURNING id. */
    protected long insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql + " RETURNING id")) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    protected static BigDecimal scalarDecimal(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal v = rs.next() ? rs.getBigDecimal(1) : null;
                return v == null ? BigDecimal.ZERO : v;
            }
        }
    }

    protected static long scalarLong(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /** Подстановка параметров с преобразованием типов java.time и перечислений. */
    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDate) {
                ps.setDate(i + 1, Date.valueOf((LocalDate) p));
            } else if (p instanceof LocalDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) p));
            } else if (p instanceof Enum) {
                ps.setString(i + 1, ((Enum<?>) p).name());
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    protected static LocalDate date(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    protected static LocalDateTime dateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static Long longOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    /** ФИО в виде «Фамилия И.О.» на стороне SQL. */
    protected static String shortNameSql(String alias) {
        return alias + ".last_name || ' ' || left(" + alias + ".first_name, 1) || '.' || coalesce(left("
                + alias + ".patronymic, 1) || '.', '')";
    }
}
