package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Модульные тесты переходов состояний договора. */
public class PolicyStatusRulesTest {
    private final List<Payment> payments = Arrays.asList(
            TestData.payment(1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
            TestData.payment(2, LocalDate.of(2026, 7, 1), null));

    @Test
    public void activeWithoutDebts() {
        assertEquals(PolicyStatus.ACTIVE, PolicyStatusRules.evaluate(TestData.policy2026(), payments, LocalDate.of(2026, 6, 1)));
    }

    @Test
    public void suspendedWhenInstallmentOverdue() {
        assertEquals(PolicyStatus.SUSPENDED,
                PolicyStatusRules.evaluate(TestData.policy2026(), payments, LocalDate.of(2026, 7, 11)));
    }

    @Test
    public void terminatedAfter30DaysOverdue() {
        assertEquals(PolicyStatus.TERMINATED,
                PolicyStatusRules.evaluate(TestData.policy2026(), payments, LocalDate.of(2026, 8, 5)));
    }

    @Test
    public void expiredAfterEndDate() {
        Payment paid = TestData.payment(2, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 30));
        assertEquals(PolicyStatus.EXPIRED, PolicyStatusRules.evaluate(TestData.policy2026(),
                Arrays.asList(payments.get(0), paid), LocalDate.of(2027, 1, 1)));
    }

    @Test
    public void waitingBecomesActiveAfterFirstPayment() {
        Policy p = TestData.policy2026();
        p.setStatus(PolicyStatus.WAITING_PAYMENT);
        assertEquals(PolicyStatus.ACTIVE, PolicyStatusRules.evaluate(p, payments, LocalDate.of(2026, 1, 2)));
    }

    @Test
    public void waitingTerminatedWhenFirstPaymentMissed() {
        Policy p = TestData.policy2026();
        p.setStatus(PolicyStatus.WAITING_PAYMENT);
        List<Payment> unpaid = Collections.singletonList(TestData.payment(1, LocalDate.of(2026, 1, 1), null));
        assertEquals(PolicyStatus.TERMINATED, PolicyStatusRules.evaluate(p, unpaid, LocalDate.of(2026, 1, 10)));
    }
}
