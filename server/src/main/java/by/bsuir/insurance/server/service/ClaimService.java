package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.exception.BusinessRuleException;
import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.ClaimStatus;
import by.bsuir.insurance.common.model.PayoutDecision;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;
import by.bsuir.insurance.common.model.User;
import by.bsuir.insurance.common.util.Money;
import by.bsuir.insurance.server.dao.ClaimDao;
import by.bsuir.insurance.server.dao.PaymentDao;
import by.bsuir.insurance.server.dao.PolicyDao;
import by.bsuir.insurance.server.db.ConnectionManager;
import by.bsuir.insurance.server.logic.PayoutCalculator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;

/** Заявления о страховых случаях и их урегулирование. */
public class ClaimService {
    private final ClaimDao claimDao = new ClaimDao();
    private final PolicyDao policyDao = new PolicyDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final PayoutCalculator calculator = new PayoutCalculator();
    private final ConnectionManager cm = ConnectionManager.getInstance();

    /** Заявление клиента о страховом случае (UC7). */
    public Claim report(User user, Claim claim) throws Exception {
        Validator.notFuture(claim.getIncidentDate(), "Дата события");
        Validator.positive(claim.getDamageAmount(), "Размер ущерба");
        claim.setDescription(Validator.required(claim.getDescription(), "Описание события"));
        claim.setReportDate(LocalDate.now());
        return cm.inTransaction(c -> {
            Policy p = policyDao.findById(c, claim.getPolicyId());
            PolicyService.checkAccess(user, p);
            if (!p.getStatus().allowsClaims()) {
                throw new BusinessRuleException("По договору в статусе «" + p.getStatus().getTitle()
                        + "» заявления не принимаются");
            }
            return claimDao.findById(c, claimDao.insert(c, claim));
        });
    }

    public ArrayList<Claim> listByClient(long clientId) throws Exception {
        return new ArrayList<>(cm.withConnection(c -> claimDao.listByClient(c, clientId)));
    }

    public ArrayList<Claim> listPending() throws Exception {
        return new ArrayList<>(cm.withConnection(c -> claimDao.listByStatus(c, ClaimStatus.REPORTED)));
    }

    public ArrayList<Claim> listAll() throws Exception {
        return new ArrayList<>(cm.withConnection(claimDao::listAll));
    }

    /** Предварительный расчёт выплаты для показа агенту. */
    public PayoutDecision preview(long claimId, BigDecimal assessedDamage) throws Exception {
        return cm.withConnection(c -> decide(c, claimDao.findById(c, claimId), assessedDamage));
    }

    /**
     * Урегулирование страхового случая (UC8): расчёт выплаты, фиксация решения и,
     * если страховая сумма исчерпана, перевод договора в состояние «Исполнен».
     */
    public Claim settle(long agentId, long claimId, BigDecimal assessedDamage, String comment) throws Exception {
        return cm.inTransaction(c -> {
            Claim claim = claimDao.lockById(c, claimId);
            PayoutDecision d = decide(c, claim, assessedDamage);
            String text = (comment == null || comment.trim().isEmpty()) ? d.getReason() : comment.trim() + ". " + d.getReason();
            claimDao.decide(c, claimId, d.isApproved() ? ClaimStatus.APPROVED : ClaimStatus.REJECTED,
                    Money.round(assessedDamage), d.getAmount(), agentId, text);
            if (d.isExhaustsSum()) {
                policyDao.updateStatus(c, claim.getPolicyId(), PolicyStatus.FULFILLED);
            }
            return claimDao.findById(c, claimId);
        });
    }

    private PayoutDecision decide(Connection c, Claim claim, BigDecimal assessedDamage) throws Exception {
        if (claim == null) {
            throw new ValidationException("Страховой случай не найден");
        }
        if (claim.getStatus() != ClaimStatus.REPORTED) {
            throw new BusinessRuleException("Решение по страховому случаю уже принято");
        }
        Validator.positive(assessedDamage, "Оценённый ущерб");
        Policy p = policyDao.lockById(c, claim.getPolicyId());
        return calculator.calculate(p, paymentDao.listByPolicy(c, p.getId()), claim, assessedDamage,
                claimDao.sumPayouts(c, p.getId()));
    }
}
