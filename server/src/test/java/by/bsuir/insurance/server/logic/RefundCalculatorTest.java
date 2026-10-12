package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.RefundCalculation;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

/** Модульные тесты расчёта возврата премии при расторжении. */
public class RefundCalculatorTest {
    private final RefundCalculator calc = new RefundCalculator();

    @Test
    public void refundProportionalToRemainingDays() {
        RefundCalculation r = calc.calculate(TestData.policy2026(), new BigDecimal("1200.00"), BigDecimal.ZERO,
                LocalDate.of(2026, 7, 2));
        assertEquals(365, r.getTotalDays());
        assertEquals(182, r.getRemainingDays());
        // 1200 × 182 / 365 × 0,75 = 448,767...
        assertEquals(new BigDecimal("448.77"), r.getRefund());
    }

    @Test
    public void payoutsReduceRefundToZero() {
        RefundCalculation r = calc.calculate(TestData.policy2026(), new BigDecimal("1200.00"), new BigDecimal("500"),
                LocalDate.of(2026, 7, 2));
        assertEquals(new BigDecimal("0.00"), r.getRefund().setScale(2));
    }
}
