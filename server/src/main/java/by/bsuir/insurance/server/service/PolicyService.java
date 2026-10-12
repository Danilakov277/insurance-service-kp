package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.exception.AuthException;
import by.bsuir.insurance.common.exception.BusinessRuleException;
import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.ControlItem;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;
import by.bsuir.insurance.common.model.PremiumQuote;
import by.bsuir.insurance.common.model.RefundCalculation;
import by.bsuir.insurance.common.model.Role;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.server.dao.ApplicationDao;
import by.bsuir.insurance.server.dao.ClaimDao;
import by.bsuir.insurance.server.dao.ClientDao;
import by.bsuir.insurance.server.dao.InsuranceTypeDao;
import by.bsuir.insurance.server.dao.PaymentDao;
import by.bsuir.insurance.server.dao.PolicyDao;
import by.bsuir.insurance.server.db.ConnectionManager;
import by.bsuir.insurance.server.logic.InstallmentScheduler;
import by.bsuir.insurance.server.logic.PolicyStatusRules;
import by.bsuir.insurance.server.logic.PremiumCalculator;
import by.bsuir.insurance.server.logic.RefundCalculator;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Сопровождение договоров: взносы, продление, расторжение, контроль. */
public class PolicyService {
    private static final Logger LOG = Logger.getLogger(PolicyService.class.getName());
    /** За сколько дней до окончания договор попадает в список истекающих и может быть продлён. */
    public static final int EXPIRING_DAYS = 30;

    private final PolicyDao policyDao = new PolicyDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final ClaimDao claimDao = new ClaimDao();
    private final ClientDao clientDao = new ClientDao();
    private final InsuranceTypeDao typeDao = new InsuranceTypeDao();
    private final ApplicationDao applicationDao = new ApplicationDao();
    private final PremiumCalculator premiumCalculator = new PremiumCalculator();
    private final RefundCalculator refundCalculator = new RefundCalculator();
    private final ConnectionManager cm = ConnectionManager.getInstance();

    public ArrayList<Policy> listByClient(long clientId) throws Exception {
        return new ArrayList<>(cm.withConnection(c -> policyDao.listByClient(c, clientId)));
    }

    public ArrayList<Policy> listAll(PolicyStatus status) throws Exception {
        return new ArrayList<>(cm.withConnection(c -> policyDao.listAll(c, status)));
    }

    /** График взносов; клиент может просматривать только свои договоры. */
    public ArrayList<Payment> payments(User user, long policyId) throws Exception {
        return cm.withConnection(c -> {
            Policy p = policyDao.findById(c, policyId);
            checkAccess(user, p);
            return new ArrayList<>(paymentDao.listByPolicy(c, policyId));
        });
    }

    /** Внесение клиентом ближайшего неуплаченного взноса (UC6). */
    public Policy payInstallment(User user, long policyId) throws Exception {
        LocalDate today = LocalDate.now();
        return cm.inTransaction(c -> {
            Policy p = policyDao.lockById(c, policyId);
            checkAccess(user, p);
            if (p.getStatus() != PolicyStatus.WAITING_PAYMENT && p.getStatus() != PolicyStatus.ACTIVE
                    && p.getStatus() != PolicyStatus.SUSPENDED) {
                throw new BusinessRuleException("По договору в статусе «" + p.getStatus().getTitle()
                        + "» взносы не принимаются");
            }
            Payment next = null;
            for (Payment pm : paymentDao.listByPolicy(c, policyId)) {
                if (!pm.isPaid()) {
                    next = pm;
                    break;
                }
            }
            if (next == null) {
                throw new BusinessRuleException("Все взносы по договору уже уплачены");
            }
            paymentDao.markPaid(c, policyId, next.getInstallmentNo(), today);
            applyRules(c, policyDao.findById(c, policyId), today);
            return policyDao.findById(c, policyId);
        });
    }

    /**
     * Продление договора (UC9): создаётся новый договор того же клиента и вида на тот же срок,
     * начинающийся на следующий день после окончания текущего; премия рассчитывается заново.
     */
    public Policy renew(long agentId, long policyId) throws Exception {
        return cm.inTransaction(c -> {
            Policy old = policyDao.lockById(c, policyId);
            if (old == null) {
                throw new ValidationException("Договор не найден");
            }
            LocalDate today = LocalDate.now();
            boolean expiringSoon = (old.getStatus() == PolicyStatus.ACTIVE)
                    && !old.getEndDate().isAfter(today.plusDays(EXPIRING_DAYS));
            if (old.getStatus() != PolicyStatus.EXPIRED && !expiringSoon) {
                throw new BusinessRuleException("Продлить можно истёкший договор или действующий договор, "
                        + "срок которого заканчивается в течение " + EXPIRING_DAYS + " дней");
            }
            if (policyDao.hasRenewal(c, old)) {
                throw new BusinessRuleException("Договор уже продлён");
            }
            int term = (int) ChronoUnit.MONTHS.between(old.getStartDate(), old.getEndDate().plusDays(1));
            LocalDate start = old.getEndDate().plusDays(1);
            if (start.isBefore(today.plusDays(1))) {
                start = today.plusDays(1);
            }
            Application params = new Application();
            params.setTypeId(old.getTypeId());
            params.setRequestedSum(old.getInsuredSum());
            params.setTermMonths(term);
            params.setFranchisePercent(old.getFranchise().signum() == 0 ? 0
                    : old.getFranchise().multiply(new java.math.BigDecimal(100))
                    .divide(old.getInsuredSum(), 0, java.math.RoundingMode.HALF_UP).intValue());
            params.setInstallments(1);
            Application source = old.getApplicationId() == null ? null : applicationDao.findById(c, old.getApplicationId());
            params.setObjectDescription(source == null ? "" : source.getObjectDescription());
            if (source != null && term % source.getInstallments() == 0) {
                params.setInstallments(source.getInstallments());
            }
            int claims = claimDao.countPaidClaims(c, old.getClientId(), start.minusYears(2), start);
            PremiumQuote q = premiumCalculator.calculate(typeDao.findById(c, old.getTypeId()), params,
                    clientDao.findById(c, old.getClientId()), start, claims);
            Policy p = new Policy();
            p.setClientId(old.getClientId());
            p.setAgentId(agentId);
            p.setTypeId(old.getTypeId());
            p.setInsuredSum(q.getInsuredSum());
            p.setPremium(q.getPremium());
            p.setFranchise(q.getFranchise());
            p.setStartDate(start);
            p.setEndDate(start.plusMonths(term).minusDays(1));
            long id = policyDao.insert(c, p);
            paymentDao.insertAll(c, InstallmentScheduler.build(id, q.getPremium(), params.getInstallments(), start, term));
            return policyDao.findById(c, id);
        });
    }

    /** Предварительный расчёт возврата при расторжении. */
    public RefundCalculation previewRefund(long policyId, LocalDate date) throws Exception {
        return cm.withConnection(c -> refund(c, checkTerminable(policyDao.findById(c, policyId)), date));
    }

    private RefundCalculation refund(Connection c, Policy p, LocalDate date) throws Exception {
        return refundCalculator.calculate(p, p.getPaidAmount(), claimDao.sumPayouts(c, p.getId()), date);
    }

    /** Досрочное расторжение с возвратом части премии (UC10). */
    public Policy terminate(long policyId) throws Exception {
        LocalDate today = LocalDate.now();
        return cm.inTransaction(c -> {
            Policy p = checkTerminable(policyDao.lockById(c, policyId));
            RefundCalculation r = refund(c, p, today);
            paymentDao.deleteUnpaidAfter(c, policyId, today);
            policyDao.terminate(c, policyId, LocalDateTime.now(), r.getRefund());
            return policyDao.findById(c, policyId);
        });
    }

    private Policy checkTerminable(Policy p) throws Exception {
        if (p == null) {
            throw new ValidationException("Договор не найден");
        }
        if (p.getStatus() != PolicyStatus.ACTIVE && p.getStatus() != PolicyStatus.SUSPENDED
                && p.getStatus() != PolicyStatus.WAITING_PAYMENT) {
            throw new BusinessRuleException("Расторгнуть можно только действующий, приостановленный "
                    + "или ожидающий оплаты договор");
        }
        return p;
    }

    /** Список контроля (UC11): просроченные взносы и договоры, истекающие в ближайшие 30 дней. */
    public ArrayList<ControlItem> controlList() throws Exception {
        LocalDate today = LocalDate.now();
        return cm.withConnection(c -> {
            ArrayList<ControlItem> items = new ArrayList<>(paymentDao.listOverdue(c, today));
            for (Policy p : policyDao.listExpiring(c, today, EXPIRING_DAYS)) {
                if (policyDao.hasRenewal(c, p)) {
                    continue;
                }
                items.add(new ControlItem(ControlItem.Kind.EXPIRING_POLICY, p.getId(), p.getPolicyNumber(),
                        p.getClientName(), clientDao.findById(c, p.getClientId()).getPhone(), p.getEndDate(),
                        p.getPremium(), ChronoUnit.DAYS.between(today, p.getEndDate())));
            }
            return items;
        });
    }

    /** Пересчёт состояний всех изменяемых договоров на текущую дату. Возвращает число изменённых. */
    public int refreshStatuses() throws Exception {
        LocalDate today = LocalDate.now();
        int changed = cm.inTransaction(c -> {
            int n = 0;
            for (Policy p : policyDao.listChangeable(c)) {
                if (applyRules(c, p, today)) {
                    n++;
                }
            }
            return n;
        });
        LOG.info("Пересчёт состояний договоров: изменено " + changed);
        return changed;
    }

    private boolean applyRules(Connection c, Policy p, LocalDate today) throws Exception {
        List<Payment> payments = paymentDao.listByPolicy(c, p.getId());
        PolicyStatus next = PolicyStatusRules.evaluate(p, payments, today);
        if (next != p.getStatus()) {
            policyDao.updateStatus(c, p.getId(), next);
            return true;
        }
        return false;
    }

    static void checkAccess(User user, Policy p) throws Exception {
        if (p == null) {
            throw new ValidationException("Договор не найден");
        }
        if (user.getRole() == Role.CLIENT && p.getClientId() != user.getPersonId()) {
            throw new AuthException("Нет доступа к чужому договору");
        }
    }
}
