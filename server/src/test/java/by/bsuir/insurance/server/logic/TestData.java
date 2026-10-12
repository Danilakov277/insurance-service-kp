package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.InsuranceCategory;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.Payment;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.model.PolicyStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Тестовые объекты для модульных тестов. */
final class TestData {
    private TestData() {
    }

    static InsuranceType type(InsuranceCategory cat, String rate, int maxTerm) {
        InsuranceType t = new InsuranceType();
        t.setId(1);
        t.setName(cat.getTitle());
        t.setCategory(cat);
        t.setBaseRate(new BigDecimal(rate));
        t.setMinSum(new BigDecimal("1000"));
        t.setMaxSum(new BigDecimal("300000"));
        t.setMaxTermMonths(maxTerm);
        t.setActive(true);
        return t;
    }

    static Client client(LocalDate birth) {
        Client c = new Client();
        c.setId(1);
        c.setLastName("Иванов");
        c.setFirstName("Сергей");
        c.setBirthDate(birth);
        return c;
    }

    static Application app(String sum, int term, int franchise, int installments, String description) {
        Application a = new Application();
        a.setRequestedSum(new BigDecimal(sum));
        a.setTermMonths(term);
        a.setFranchisePercent(franchise);
        a.setInstallments(installments);
        a.setObjectDescription(description);
        return a;
    }

    /** Договор на 2026 год: сумма 10 000, франшиза 100. */
    static Policy policy2026() {
        Policy p = new Policy();
        p.setId(7);
        p.setPolicyNumber("INS-2026-000007");
        p.setInsuredSum(new BigDecimal("10000.00"));
        p.setPremium(new BigDecimal("1200.00"));
        p.setFranchise(new BigDecimal("100.00"));
        p.setStartDate(LocalDate.of(2026, 1, 1));
        p.setEndDate(LocalDate.of(2026, 12, 31));
        p.setStatus(PolicyStatus.ACTIVE);
        p.setCreatedAt(LocalDateTime.of(2025, 12, 28, 10, 0));
        return p;
    }

    static Payment payment(int no, LocalDate due, LocalDate paid) {
        Payment p = new Payment(7, no, new BigDecimal("600.00"), due);
        p.setPaidDate(paid);
        return p;
    }
}
