package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Payment;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Модульные тесты формирования графика взносов. */
public class InstallmentSchedulerTest {

    @Test
    public void monthlyScheduleKeepsTotalAndDates() throws Exception {
        List<Payment> list = InstallmentScheduler.build(1, new BigDecimal("100.00"), 12,
                LocalDate.of(2026, 1, 31), 12);
        assertEquals(12, list.size());
        assertEquals(new BigDecimal("8.33"), list.get(0).getAmount());
        assertEquals(new BigDecimal("8.37"), list.get(11).getAmount());
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : list) {
            total = total.add(p.getAmount());
        }
        assertEquals(new BigDecimal("100.00"), total);
        assertEquals(LocalDate.of(2026, 2, 28), list.get(1).getDueDate());
        assertEquals(LocalDate.of(2026, 12, 31), list.get(11).getDueDate());
    }

    @Test
    public void quarterlySchedule() throws Exception {
        List<Payment> list = InstallmentScheduler.build(1, new BigDecimal("1676.70"), 4, LocalDate.of(2026, 10, 14), 12);
        assertEquals(new BigDecimal("419.17"), list.get(0).getAmount());
        assertEquals(new BigDecimal("419.19"), list.get(3).getAmount());
        assertEquals(LocalDate.of(2027, 4, 14), list.get(2).getDueDate());
    }

    @Test(expected = ValidationException.class)
    public void termNotDivisibleRejected() throws Exception {
        InstallmentScheduler.build(1, new BigDecimal("100.00"), 4, LocalDate.of(2026, 1, 1), 6);
    }

    @Test(expected = ValidationException.class)
    public void wrongInstallmentsRejected() throws Exception {
        InstallmentScheduler.validate(12, 3);
    }
}
