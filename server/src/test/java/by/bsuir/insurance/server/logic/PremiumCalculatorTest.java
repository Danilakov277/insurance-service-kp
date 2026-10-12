package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.InsuranceCategory;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.PremiumQuote;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

/** Модульные тесты алгоритма расчёта страховой премии. */
public class PremiumCalculatorTest {
    private final PremiumCalculator calc = new PremiumCalculator();
    private final LocalDate start = LocalDate.of(2026, 10, 14);

    @Test
    public void propertyStandardClientWithoutClaims() throws Exception {
        PremiumQuote q = calc.calculate(TestData.type(InsuranceCategory.PROPERTY, "0.0050", 36),
                TestData.app("50000", 12, 0, 1, "Квартира, 2 комнаты"),
                TestData.client(LocalDate.of(1985, 4, 12)), start, 0);
        // 50000 × 0,005 × 1,00 × 1,00 × 0,90 × 1,00 × 1,00
        assertEquals(new BigDecimal("225.00"), q.getPremium());
        assertEquals(new BigDecimal("0.00"), q.getFranchise());
    }

    @Test
    public void cascoYoungDriverWithFranchise() throws Exception {
        PremiumQuote q = calc.calculate(TestData.type(InsuranceCategory.CASCO, "0.0450", 12),
                TestData.app("30000", 12, 2, 4, "Volkswagen Polo"),
                TestData.client(LocalDate.of(2003, 5, 30)), start, 1);
        // 30000 × 0,045 × 1,20 × 1,00 × 1,00 × 0,90 × 1,15
        assertEquals(new BigDecimal("1676.70"), q.getPremium());
        assertEquals(new BigDecimal("600.00"), q.getFranchise());
    }

    @Test
    public void travelShortTermSeniorActiveRest() throws Exception {
        PremiumQuote q = calc.calculate(TestData.type(InsuranceCategory.TRAVEL, "0.0300", 12),
                TestData.app("20000", 1, 0, 1, "Горнолыжный отдых"),
                TestData.client(LocalDate.of(1955, 3, 5)), start, 2);
        // 20000 × 0,03 × 1,35 × (1/12 × 1,1) × 1,25 × 1,00 × 1,50 = 139,21875
        assertEquals(new BigDecimal("139.22"), q.getPremium());
    }

    @Test
    public void largeSumPropertyDiscount() throws Exception {
        PremiumQuote q = calc.calculate(TestData.type(InsuranceCategory.PROPERTY, "0.0050", 36),
                TestData.app("200000", 24, 5, 2, "Деревянный дом"),
                TestData.client(LocalDate.of(1985, 4, 12)), start, 0);
        // 200000 × 0,005 × 1,00 × 2 × 0,90 × 0,80 × (0,95 × 1,20)
        assertEquals(new BigDecimal("1641.60"), q.getPremium());
    }

    @Test(expected = ValidationException.class)
    public void sumOutOfRangeRejected() throws Exception {
        calc.calculate(TestData.type(InsuranceCategory.PROPERTY, "0.0050", 36),
                TestData.app("500", 12, 0, 1, "Квартира"), TestData.client(LocalDate.of(1985, 4, 12)), start, 0);
    }

    @Test(expected = ValidationException.class)
    public void termOverMaximumRejected() throws Exception {
        calc.calculate(TestData.type(InsuranceCategory.CASCO, "0.0450", 12),
                TestData.app("30000", 24, 0, 1, "Авто"), TestData.client(LocalDate.of(1985, 4, 12)), start, 0);
    }

    @Test(expected = ValidationException.class)
    public void inactiveTypeRejected() throws Exception {
        InsuranceType t = TestData.type(InsuranceCategory.PROPERTY, "0.0050", 36);
        t.setActive(false);
        calc.calculate(t, TestData.app("50000", 12, 0, 1, "Квартира"),
                TestData.client(LocalDate.of(1985, 4, 12)), start, 0);
    }

    @Test
    public void factorsFollowTariffTables() {
        assertEquals(new BigDecimal("1.20"), PremiumCalculator.ageFactor(24));
        assertEquals(new BigDecimal("1.00"), PremiumCalculator.ageFactor(25));
        assertEquals(new BigDecimal("1.00"), PremiumCalculator.ageFactor(60));
        assertEquals(new BigDecimal("1.35"), PremiumCalculator.ageFactor(61));
        assertEquals(new BigDecimal("0.90"), PremiumCalculator.bonusMalusFactor(0));
        assertEquals(new BigDecimal("1.25"), PremiumCalculator.bonusMalusFactor(3));
        assertEquals(new BigDecimal("0.80"), PremiumCalculator.franchiseFactor(5));
        assertEquals(0, new BigDecimal("2").compareTo(PremiumCalculator.termFactor(24)));
    }
}
