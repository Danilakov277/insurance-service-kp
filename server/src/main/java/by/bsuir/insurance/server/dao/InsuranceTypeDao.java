package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.InsuranceCategory;
import by.bsuir.insurance.common.model.InsuranceType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** Доступ к видам страхования и тарифам. */
public class InsuranceTypeDao extends AbstractDao<InsuranceType> {

    @Override
    protected InsuranceType map(ResultSet rs) throws SQLException {
        InsuranceType t = new InsuranceType();
        t.setId(rs.getLong("id"));
        t.setName(rs.getString("name"));
        t.setCategory(InsuranceCategory.valueOf(rs.getString("category")));
        t.setDescription(rs.getString("description"));
        t.setBaseRate(rs.getBigDecimal("base_rate"));
        t.setMinSum(rs.getBigDecimal("min_sum"));
        t.setMaxSum(rs.getBigDecimal("max_sum"));
        t.setMaxTermMonths(rs.getInt("max_term_months"));
        t.setActive(rs.getBoolean("is_active"));
        return t;
    }

    public List<InsuranceType> listAll(Connection c) throws SQLException {
        return list(c, "SELECT * FROM insurance_types ORDER BY id");
    }

    public List<InsuranceType> listActive(Connection c) throws SQLException {
        return list(c, "SELECT * FROM insurance_types WHERE is_active ORDER BY id");
    }

    public InsuranceType findById(Connection c, long id) throws SQLException {
        return one(c, "SELECT * FROM insurance_types WHERE id = ?", id);
    }

    public boolean nameExists(Connection c, String name, long exceptId) throws SQLException {
        return scalarLong(c, "SELECT count(*) FROM insurance_types WHERE lower(name) = lower(?) AND id <> ?",
                name, exceptId) > 0;
    }

    public long insert(Connection c, InsuranceType t) throws SQLException {
        return insert(c, "INSERT INTO insurance_types (name, category, description, base_rate, min_sum, max_sum, "
                        + "max_term_months, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                t.getName(), t.getCategory(), t.getDescription(), t.getBaseRate(), t.getMinSum(), t.getMaxSum(),
                t.getMaxTermMonths(), t.isActive());
    }

    public void update(Connection c, InsuranceType t) throws SQLException {
        update(c, "UPDATE insurance_types SET name = ?, category = ?, description = ?, base_rate = ?, min_sum = ?, "
                        + "max_sum = ?, max_term_months = ?, is_active = ? WHERE id = ?",
                t.getName(), t.getCategory(), t.getDescription(), t.getBaseRate(), t.getMinSum(), t.getMaxSum(),
                t.getMaxTermMonths(), t.isActive(), t.getId());
    }
}
