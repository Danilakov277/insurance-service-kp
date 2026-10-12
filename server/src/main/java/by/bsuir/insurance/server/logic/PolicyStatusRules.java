package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Правила автоматической смены состояний договора (см. диаграмму состояний). */
public final class PolicyStatusRules {
    /** Срок оплаты первого взноса после оформления, дней. */
    public static final int FIRST_PAYMENT_DAYS = 10;
    /** Допустимая просрочка очередного взноса до расторжения, дней. */
    public static final int MAX_OVERDUE_DAYS = 30;

    private PolicyStatusRules() {
    }

    /** Новое состояние договора на дату today (или текущее, если переход не нужен). */
    public static PolicyStatus evaluate(Policy policy, List<Payment> payments, LocalDate today) {
        PolicyStatus status = policy.getStatus();
        if (status == PolicyStatus.TERMINATED || status == PolicyStatus.FULFILLED || status == PolicyStatus.EXPIRED) {
            return status;
        }
        if (status == PolicyStatus.WAITING_PAYMENT) {
            Payment first = payments.isEmpty() ? null : payments.get(0);
            if (first != null && first.isPaid()) {
                return PolicyStatus.ACTIVE;
            }
            LocalDate created = policy.getCreatedAt().toLocalDate();
            return ChronoUnit.DAYS.between(created, today) > FIRST_PAYMENT_DAYS ? PolicyStatus.TERMINATED : status;
        }
        if (today.isAfter(policy.getEndDate())) {
            return PolicyStatus.EXPIRED;
        }
        long maxOverdue = 0;
        for (Payment p : payments) {
            if (p.isOverdue(today)) {
                maxOverdue = Math.max(maxOverdue, ChronoUnit.DAYS.between(p.getDueDate(), today));
            }
        }
        if (maxOverdue > MAX_OVERDUE_DAYS) {
            return PolicyStatus.TERMINATED;
        }
        return maxOverdue > 0 ? PolicyStatus.SUSPENDED : PolicyStatus.ACTIVE;
    }
}
