package by.bsuir.insurance.server.dao;

import by.bsuir.insurance.common.model.Client;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Доступ к данным клиентов. */
public class ClientDao extends AbstractDao<Client> {

    @Override
    protected Client map(ResultSet rs) throws SQLException {
        Client c = new Client();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setLastName(rs.getString("last_name"));
        c.setFirstName(rs.getString("first_name"));
        c.setPatronymic(rs.getString("patronymic"));
        c.setBirthDate(date(rs, "birth_date"));
        c.setPassportNo(rs.getString("passport_no"));
        c.setPhone(rs.getString("phone"));
        c.setAddress(rs.getString("address"));
        return c;
    }

    public Client findById(Connection c, long id) throws SQLException {
        return one(c, "SELECT * FROM clients WHERE id = ?", id);
    }

    public boolean passportExists(Connection c, String passport) throws SQLException {
        return scalarLong(c, "SELECT count(*) FROM clients WHERE upper(passport_no) = upper(?)", passport) > 0;
    }

    public long insert(Connection c, Client cl) throws SQLException {
        return insert(c, "INSERT INTO clients (user_id, last_name, first_name, patronymic, birth_date, passport_no, "
                        + "phone, address) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                cl.getUserId(), cl.getLastName(), cl.getFirstName(), cl.getPatronymic(), cl.getBirthDate(),
                cl.getPassportNo().toUpperCase(), cl.getPhone(), cl.getAddress());
    }
}
