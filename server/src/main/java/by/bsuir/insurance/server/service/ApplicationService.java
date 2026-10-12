package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.exception.BusinessRuleException;
import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.ApplicationStatus;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PremiumQuote;
import by.bsuir.insurance.server.dao.ApplicationDao;
import by.bsuir.insurance.server.dao.ClaimDao;
import by.bsuir.insurance.server.dao.ClientDao;
import by.bsuir.insurance.server.dao.InsuranceTypeDao;
import by.bsuir.insurance.server.dao.PaymentDao;
import by.bsuir.insurance.server.dao.PolicyDao;
import by.bsuir.insurance.server.db.ConnectionManager;
import by.bsuir.insurance.server.logic.InstallmentScheduler;
import by.bsuir.insurance.server.logic.PremiumCalculator;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Расчёт стоимости, заявки и оформление договоров по заявкам. */
public class ApplicationService {
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final InsuranceTypeDao typeDao = new InsuranceTypeDao();
    private final ClientDao clientDao = new ClientDao();
    private final ClaimDao claimDao = new ClaimDao();
    private final PolicyDao policyDao = new PolicyDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final PremiumCalculator calculator = new PremiumCalculator();
    private final ConnectionManager cm = ConnectionManager.getInstance();

    /** Расчёт стоимости страхования для клиента (UC3). */
    public PremiumQuote quote(long clientId, Application a, LocalDate startDate) throws Exception {
        return cm.withConnection(c -> quote(c, clientId, a, startDate));
    }

    private PremiumQuote quote(Connection c, long clientId, Application a, LocalDate startDate) throws Exception {
        InsuranceType type = typeDao.findById(c, a.getTypeId());
        if (type == null) {
            throw new ValidationException("Вид страхования не найден");
        }
        Client client = clientDao.findById(c, clientId);
        int claims = claimDao.countPaidClaims(c, clientId, startDate.minusYears(2), startDate);
        return calculator.calculate(type, a, client, startDate, claims);
    }

    /** Подача заявки клиентом (UC4). Параметры проверяются тем же калькулятором. */
    public Application submit(long clientId, Application a) throws Exception {
        a.setClientId(clientId);
        a.setObjectDescription(Validator.required(a.getObjectDescription(), "Описание объекта страхования"));
        return cm.inTransaction(c -> {
            quote(c, clientId, a, LocalDate.now().plusDays(1));
            long id = applicationDao.insert(c, a);
            return applicationDao.findById(c, id);
        });
    }

    public ArrayList<Application> listByClient(long clientId) throws Exception {
        return new ArrayList<>(cm.withConnection(c -> applicationDao.listByClient(c, clientId)));
    }

    /** Новые заявки с предварительным расчётом премии. */
    public ArrayList<Application> listNew() throws Exception {
        return cm.withConnection(c -> {
            List<Application> list = applicationDao.listByStatus(c, ApplicationStatus.NEW);
            for (Application a : list) {
                try {
                    a.setEstimatedPremium(quote(c, a.getClientId(), a, LocalDate.now().plusDays(1)).getPremium());
                } catch (ValidationException e) {
                    a.setEstimatedPremium(null);
                }
            }
            return new ArrayList<>(list);
        });
    }

    /**
     * Одобрение заявки и оформление договора (UC5): в одной транзакции рассчитывается премия,
     * создаются договор и график взносов, заявка переводится в статус «Одобрена».
     */
    public Policy approve(long agentId, long applicationId, LocalDate startDate) throws Exception {
        if (startDate == null || startDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Дата начала действия договора не может быть в прошлом");
        }
        return cm.inTransaction(c -> {
            Application a = applicationDao.lockById(c, applicationId);
            if (a == null) {
                throw new ValidationException("Заявка не найдена");
            }
            if (a.getStatus() != ApplicationStatus.NEW) {
                throw new BusinessRuleException("Заявка уже рассмотрена: " + a.getStatus().getTitle());
            }
            PremiumQuote q = quote(c, a.getClientId(), a, startDate);
            Policy p = new Policy();
            p.setApplicationId(a.getId());
            p.setClientId(a.getClientId());
            p.setAgentId(agentId);
            p.setTypeId(a.getTypeId());
            p.setInsuredSum(q.getInsuredSum());
            p.setPremium(q.getPremium());
            p.setFranchise(q.getFranchise());
            p.setStartDate(startDate);
            p.setEndDate(startDate.plusMonths(a.getTermMonths()).minusDays(1));
            long policyId = policyDao.insert(c, p);
            List<Payment> schedule = InstallmentScheduler.build(policyId, q.getPremium(), a.getInstallments(),
                    startDate, a.getTermMonths());
            paymentDao.insertAll(c, schedule);
            applicationDao.decide(c, a.getId(), ApplicationStatus.APPROVED, agentId,
                    "Оформлен договор " + p.getPolicyNumber());
            return policyDao.findById(c, policyId);
        });
    }

    public Application reject(long agentId, long applicationId, String comment) throws Exception {
        String reason = Validator.required(comment, "Причина отказа");
        return cm.inTransaction(c -> {
            Application a = applicationDao.lockById(c, applicationId);
            if (a == null) {
                throw new ValidationException("Заявка не найдена");
            }
            if (a.getStatus() != ApplicationStatus.NEW) {
                throw new BusinessRuleException("Заявка уже рассмотрена: " + a.getStatus().getTitle());
            }
            applicationDao.decide(c, applicationId, ApplicationStatus.REJECTED, agentId, reason);
            return applicationDao.findById(c, applicationId);
        });
    }
}
