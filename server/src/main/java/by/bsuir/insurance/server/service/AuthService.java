package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.exception.AuthException;
import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.Role;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.server.dao.ClientDao;
import by.bsuir.insurance.server.dao.UserDao;
import by.bsuir.insurance.server.db.ConnectionManager;
import by.bsuir.insurance.server.security.PasswordUtil;

import java.time.LocalDate;

/** Авторизация и регистрация клиентов. */
public class AuthService {
    private final UserDao userDao = new UserDao();
    private final ClientDao clientDao = new ClientDao();

    public User login(String login, String password) throws Exception {
        if (login == null || password == null) {
            throw new AuthException("Введите логин и пароль");
        }
        UserDao.Credentials cr = ConnectionManager.getInstance().withConnection(c -> userDao.findCredentials(c, login));
        if (cr == null || !PasswordUtil.matches(password, cr.salt, cr.hash)) {
            throw new AuthException("Неверный логин или пароль");
        }
        if (cr.user.isBlocked()) {
            throw new AuthException("Учётная запись заблокирована. Обратитесь к администратору");
        }
        return cr.user;
    }

    /** Регистрация клиента: учётная запись и данные клиента создаются в одной транзакции. */
    public User registerClient(String login, String password, Client data) throws Exception {
        String lg = Validator.login(login);
        Validator.password(password);
        data.setLastName(Validator.name(data.getLastName(), "Фамилия", true));
        data.setFirstName(Validator.name(data.getFirstName(), "Имя", true));
        data.setPatronymic(Validator.name(data.getPatronymic(), "Отчество", false));
        data.setPassportNo(Validator.passport(data.getPassportNo()));
        data.setPhone(Validator.phone(data.getPhone()));
        data.setAddress(Validator.required(data.getAddress(), "Адрес"));
        if (data.getBirthDate() == null) {
            throw new ValidationException("Укажите дату рождения");
        }
        if (data.getAge(LocalDate.now()) < 18) {
            throw new ValidationException("Заключать договоры могут лица, достигшие 18 лет");
        }
        long userId = ConnectionManager.getInstance().inTransaction(c -> {
            if (userDao.loginExists(c, lg)) {
                throw new ValidationException("Логин «" + lg + "» уже занят");
            }
            if (clientDao.passportExists(c, data.getPassportNo())) {
                throw new ValidationException("Клиент с таким номером паспорта уже зарегистрирован");
            }
            String salt = PasswordUtil.newSalt();
            long id = userDao.insert(c, lg, PasswordUtil.hash(password, salt), salt, Role.CLIENT);
            data.setUserId(id);
            clientDao.insert(c, data);
            return id;
        });
        return ConnectionManager.getInstance().withConnection(c -> userDao.findById(c, userId));
    }

    public Client profile(long clientId) throws Exception {
        return ConnectionManager.getInstance().withConnection(c -> clientDao.findById(c, clientId));
    }
}
