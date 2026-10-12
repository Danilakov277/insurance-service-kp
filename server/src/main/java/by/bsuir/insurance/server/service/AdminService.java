package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.exception.BusinessRuleException;
import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Agent;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.Role;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.server.dao.AgentDao;
import by.bsuir.insurance.server.dao.InsuranceTypeDao;
import by.bsuir.insurance.server.dao.UserDao;
import by.bsuir.insurance.server.db.ConnectionManager;
import by.bsuir.insurance.server.security.PasswordUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

/** Справочник видов страхования и учётные записи (функции администратора). */
public class AdminService {
    private final InsuranceTypeDao typeDao = new InsuranceTypeDao();
    private final AgentDao agentDao = new AgentDao();
    private final UserDao userDao = new UserDao();
    private final ConnectionManager cm = ConnectionManager.getInstance();

    public ArrayList<InsuranceType> activeTypes() throws Exception {
        return new ArrayList<>(cm.withConnection(typeDao::listActive));
    }

    public ArrayList<InsuranceType> allTypes() throws Exception {
        return new ArrayList<>(cm.withConnection(typeDao::listAll));
    }

    public InsuranceType saveType(InsuranceType t) throws Exception {
        t.setName(Validator.required(t.getName(), "Наименование"));
        if (t.getCategory() == null) {
            throw new ValidationException("Выберите категорию вида страхования");
        }
        BigDecimal rate = Validator.positive(t.getBaseRate(), "Базовый тариф");
        if (rate.compareTo(BigDecimal.ONE) >= 0) {
            throw new ValidationException("Базовый тариф задаётся долей страховой суммы, например 0,0050");
        }
        Validator.positive(t.getMinSum(), "Минимальная сумма");
        Validator.positive(t.getMaxSum(), "Максимальная сумма");
        if (t.getMaxSum().compareTo(t.getMinSum()) < 0) {
            throw new ValidationException("Максимальная сумма меньше минимальной");
        }
        if (t.getMaxTermMonths() < 1 || t.getMaxTermMonths() > 60) {
            throw new ValidationException("Максимальный срок — от 1 до 60 месяцев");
        }
        return cm.inTransaction(c -> {
            if (typeDao.nameExists(c, t.getName(), t.getId())) {
                throw new ValidationException("Вид страхования с таким наименованием уже есть");
            }
            if (t.isNew()) {
                t.setId(typeDao.insert(c, t));
            } else {
                typeDao.update(c, t);
            }
            return typeDao.findById(c, t.getId());
        });
    }

    public ArrayList<Agent> agents() throws Exception {
        return new ArrayList<>(cm.withConnection(agentDao::listAll));
    }

    public Agent createAgent(String login, String password, Agent a) throws Exception {
        String lg = Validator.login(login);
        Validator.password(password);
        a.setLastName(Validator.name(a.getLastName(), "Фамилия", true));
        a.setFirstName(Validator.name(a.getFirstName(), "Имя", true));
        a.setPatronymic(Validator.name(a.getPatronymic(), "Отчество", false));
        a.setPosition(Validator.required(a.getPosition(), "Должность"));
        if (a.getHireDate() == null) {
            a.setHireDate(LocalDate.now());
        }
        BigDecimal rate = a.getCommissionRate();
        if (rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("0.5")) > 0) {
            throw new ValidationException("Ставка вознаграждения — от 0 до 0,5");
        }
        return cm.inTransaction(c -> {
            if (userDao.loginExists(c, lg)) {
                throw new ValidationException("Логин «" + lg + "» уже занят");
            }
            String salt = PasswordUtil.newSalt();
            a.setUserId(userDao.insert(c, lg, PasswordUtil.hash(password, salt), salt, Role.AGENT));
            return agentDao.findById(c, agentDao.insert(c, a));
        });
    }

    public ArrayList<User> users() throws Exception {
        return new ArrayList<>(cm.withConnection(userDao::listAll));
    }

    public User setBlocked(User admin, long userId, boolean blocked) throws Exception {
        return cm.inTransaction(c -> {
            User u = userDao.findById(c, userId);
            if (u == null) {
                throw new ValidationException("Пользователь не найден");
            }
            if (u.getRole() == Role.ADMIN) {
                throw new BusinessRuleException("Учётную запись администратора заблокировать нельзя");
            }
            userDao.setBlocked(c, userId, blocked);
            return userDao.findById(c, userId);
        });
    }
}
