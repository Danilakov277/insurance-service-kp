package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.RefundCalculation;
import by.bsuir.insurance.common.util.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Возврат части премии при досрочном расторжении:
 * В = уплачено × (неистёкшие дни / все дни) × (1 − доля расходов) − выплаты, но не меньше 0.
 */
public class RefundCalculator {
    /** Доля расходов на ведение дела, удерживаемая страховщиком. */
    public static final BigDecimal EXPENSES_SHARE = new BigDecimal("0.25");

    public RefundCalculation calculate(Policy policy, BigDecimal paid, BigDecimal payouts, LocalDate terminationDate) {
        long total = ChronoUnit.DAYS.between(policy.getStartDate(), policy.getEndDate()) + 1;
        long remaining = ChronoUnit.DAYS.between(terminationDate, policy.getEndDate());
        if (terminationDate.isBefore(policy.getStartDate())) {
            remaining = total;
        }
        remaining = Math.max(0, Math.min(total, remaining));
        BigDecimal refund = Money.nz(paid)
                .multiply(new BigDecimal(remaining))
                .divide(new BigDecimal(total), MathContext.DECIMAL64)
                .multiply(BigDecimal.ONE.subtract(EXPENSES_SHARE))
                .subtract(Money.nz(payouts));
        if (refund.signum() < 0) {
            refund = BigDecimal.ZERO;
        }
        return new RefundCalculation(Money.nz(paid), total, remaining, EXPENSES_SHARE, Money.nz(payouts), Money.round(refund));
    }
}
