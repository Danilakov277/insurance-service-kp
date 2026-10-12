package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.PayoutDecision;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Модульные тесты алгоритма расчёта страховой выплаты (все ветви схемы алгоритма). */
public class PayoutCalculatorTest {
    private final PayoutCalculator calc = new PayoutCalculator();
    private final List<Payment> payments = Arrays.asList(
            TestData.payment(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
            TestData.payment(2, LocalDate.of(2026, 7, 1), null));

    private static Claim claim(LocalDate incident, LocalDate report) {
        Claim c = new Claim();
        c.setIncidentDate(incident);
        c.setReportDate(report);
        return c;
    }

    @Test
    public void payoutIsDamageMinusFranchise() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)), new BigDecimal("1500"), BigDecimal.ZERO);
        assertTrue(d.isApproved());
        assertEquals(new BigDecimal("1400.00"), d.getAmount());
        assertFalse(d.isExhaustsSum());
    }

    @Test
    public void rejectedWhenOutsidePeriod() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2025, 12, 20), LocalDate.of(2025, 12, 22)), new BigDecimal("1500"), BigDecimal.ZERO);
        assertFalse(d.isApproved());
        assertTrue(d.getReason().contains("вне периода"));
    }

    @Test
    public void rejectedWhenPremiumOverdue() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 11)), new BigDecimal("1500"), BigDecimal.ZERO);
        assertFalse(d.isApproved());
        assertTrue(d.getReason().contains("просроченные"));
    }

    @Test
    public void rejectedWhenReportedTooLate() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 4, 24)), new BigDecimal("1500"), BigDecimal.ZERO);
        assertFalse(d.isApproved());
        assertTrue(d.getReason().contains("30 дней"));
    }

    @Test
    public void rejectedWhenDamageWithinFranchise() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)), new BigDecimal("80"), BigDecimal.ZERO);
        assertFalse(d.isApproved());
        assertTrue(d.getReason().contains("франшизу"));
    }

    @Test
    public void payoutLimitedByRemainingSum() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)), new BigDecimal("2000"),
                new BigDecimal("9500"));
        assertTrue(d.isApproved());
        assertEquals(new BigDecimal("500.00"), d.getAmount());
        assertTrue(d.isExhaustsSum());
    }

    @Test
    public void rejectedWhenSumExhausted() {
        PayoutDecision d = calc.calculate(TestData.policy2026(), payments,
                claim(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)), new BigDecimal("2000"),
                new BigDecimal("10000"));
        assertFalse(d.isApproved());
    }
}
