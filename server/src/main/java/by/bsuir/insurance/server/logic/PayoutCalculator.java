package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.PayoutDecision;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Алгоритм расчёта страховой выплаты (функция A44 модели IDEF0).
 */
public class PayoutCalculator {
    /** Максимальный срок подачи заявления, дней. */
    public static final int REPORT_DEADLINE_DAYS = 30;

    /**
     * @param policy       договор
     * @param payments     график взносов договора
     * @param claim        страховой случай
     * @param damage       оценённый агентом размер ущерба
     * @param paidBefore   сумма выплат по договору до этого случая
     */
    public PayoutDecision calculate(Policy policy, List<Payment> payments, Claim claim,
                                    BigDecimal damage, BigDecimal paidBefore) {
        List<String> steps = new ArrayList<>();
        LocalDate incident = claim.getIncidentDate();

        // 1. Договор действовал на дату события
        steps.add("Период действия договора: " + policy.getStartDate() + " – " + policy.getEndDate());
        if (!policy.isActiveOn(incident)) {
            return PayoutDecision.reject("событие произошло вне периода действия договора", steps);
        }
        LocalDateTime terminatedAt = policy.getTerminatedAt();
        if (terminatedAt != null && !incident.isBefore(terminatedAt.toLocalDate())) {
            return PayoutDecision.reject("договор был расторгнут до даты события", steps);
        }
        // 2. Нет просроченных взносов на дату события
        for (Payment p : payments) {
            if (p.isOverdue(incident)) {
                steps.add("Взнос № " + p.getInstallmentNo() + " со сроком " + p.getDueDate() + " не был уплачен");
                return PayoutDecision.reject("на дату события имелись просроченные страховые взносы", steps);
            }
        }
        steps.add("Просроченных взносов на дату события нет");
        // 3. Срок подачи заявления
        long days = ChronoUnit.DAYS.between(incident, claim.getReportDate());
        steps.add("Заявление подано через " + days + " дн. после события");
        if (days > REPORT_DEADLINE_DAYS) {
            return PayoutDecision.reject("заявление подано позднее " + REPORT_DEADLINE_DAYS + " дней с даты события", steps);
        }
        // 4. Франшиза
        BigDecimal franchise = Money.nz(policy.getFranchise());
        steps.add("Ущерб " + Money.format(damage) + ", франшиза " + Money.format(franchise));
        if (damage.compareTo(franchise) <= 0) {
            return PayoutDecision.reject("размер ущерба не превышает франшизу", steps);
        }
        BigDecimal payout = damage.subtract(franchise);
        // 5. Ограничение остатком страховой суммы
        BigDecimal remaining = policy.getInsuredSum().subtract(Money.nz(paidBefore));
        steps.add("Остаток страховой суммы: " + Money.format(remaining));
        if (remaining.signum() <= 0) {
            return PayoutDecision.reject("страховая сумма исчерпана предыдущими выплатами", steps);
        }
        boolean exhausts = false;
        if (payout.compareTo(remaining) >= 0) {
            payout = remaining;
            exhausts = true;
            steps.add("Выплата ограничена остатком страховой суммы, договор будет исполнен");
        }
        payout = Money.round(payout);
        steps.add("Сумма выплаты: " + Money.format(payout));
        return PayoutDecision.approve(payout, exhausts, steps);
    }
}
