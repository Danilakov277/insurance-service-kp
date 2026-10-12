package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Client;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.PremiumQuote;
import by.bsuir.insurance.common.util.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

/**
 * Расчёт страховой премии: П = С × Т × Кв × Кс × Кбм × Кф × Кт.
 */
public class PremiumCalculator {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE = new BigDecimal("12");

    /**
     * Полный расчёт по заявке.
     *
     * @param type         вид страхования
     * @param application  параметры страхования (сумма, срок, франшиза, описание объекта)
     * @param client       страхователь
     * @param startDate    дата начала действия договора
     * @param claimsIn2Years число страховых выплат клиенту за последние два года
     */
    public PremiumQuote calculate(InsuranceType type, Application application, Client client,
                                  LocalDate startDate, int claimsIn2Years) throws ValidationException {
        validate(type, application);
        int age = client.getAge(startDate);
        BigDecimal sum = application.getRequestedSum();

        PremiumQuote quote = new PremiumQuote();
        quote.setInsuredSum(Money.round(sum));
        quote.setBaseRate(type.getBaseRate());

        BigDecimal kAge = ageFactor(age);
        BigDecimal kTerm = termFactor(application.getTermMonths());
        BigDecimal kBonus = bonusMalusFactor(claimsIn2Years);
        BigDecimal kFranchise = franchiseFactor(application.getFranchisePercent());
        PremiumStrategy strategy = PremiumStrategyFactory.forType(type);
        BigDecimal kType = strategy.typeFactor(application, client, age);

        quote.addFactor("Кв (возраст " + age + ")", kAge);
        quote.addFactor("Кс (срок " + application.getTermMonths() + " мес.)", kTerm);
        quote.addFactor("Кбм (выплат за 2 года: " + claimsIn2Years + ")", kBonus);
        quote.addFactor("Кф (франшиза " + application.getFranchisePercent() + " %)", kFranchise);
        quote.addFactor(strategy.factorName(), kType);

        BigDecimal premium = sum.multiply(type.getBaseRate(), MC)
                .multiply(kAge, MC).multiply(kTerm, MC).multiply(kBonus, MC)
                .multiply(kFranchise, MC).multiply(kType, MC);
        quote.setPremium(Money.round(premium));
        quote.setFranchise(franchiseAmount(sum, application.getFranchisePercent()));
        return quote;
    }

    /** Упрощённый расчёт без истории клиента (перегрузка для калькулятора). */
    public BigDecimal calculate(InsuranceType type, BigDecimal sum, int termMonths, int age) {
        BigDecimal p = sum.multiply(type.getBaseRate(), MC).multiply(ageFactor(age), MC)
                .multiply(termFactor(termMonths), MC);
        return Money.round(p);
    }

    public void validate(InsuranceType type, Application a) throws ValidationException {
        if (!type.isActive()) {
            throw new ValidationException("Вид страхования «" + type.getName() + "» недоступен для новых договоров");
        }
        BigDecimal sum = a.getRequestedSum();
        if (sum == null || sum.compareTo(type.getMinSum()) < 0 || sum.compareTo(type.getMaxSum()) > 0) {
            throw new ValidationException("Страховая сумма должна быть от " + Money.format(type.getMinSum())
                    + " до " + Money.format(type.getMaxSum()) + " BYN");
        }
        if (a.getTermMonths() < 1 || a.getTermMonths() > type.getMaxTermMonths()) {
            throw new ValidationException("Срок страхования должен быть от 1 до " + type.getMaxTermMonths() + " мес.");
        }
        InstallmentScheduler.validate(a.getTermMonths(), a.getInstallments());
        int f = a.getFranchisePercent();
        if (f != 0 && f != 1 && f != 2 && f != 5) {
            throw new ValidationException("Допустимая франшиза: 0, 1, 2 или 5 %");
        }
    }

    /** Возрастной коэффициент Кв. */
    public static BigDecimal ageFactor(int age) {
        if (age < 25) {
            return new BigDecimal("1.20");
        }
        if (age > 60) {
            return new BigDecimal("1.35");
        }
        return new BigDecimal("1.00");
    }

    /** Коэффициент срока Кс: доля года, для краткосрочных договоров надбавка 10 %. */
    public static BigDecimal termFactor(int termMonths) {
        BigDecimal k = new BigDecimal(termMonths).divide(TWELVE, MC);
        if (termMonths < 12) {
            k = k.multiply(new BigDecimal("1.10"), MC);
        }
        return k;
    }

    /** Коэффициент бонус-малус Кбм по числу выплат за два года. */
    public static BigDecimal bonusMalusFactor(int claims) {
        if (claims == 0) {
            return new BigDecimal("0.90");
        }
        return claims == 1 ? new BigDecimal("1.00") : new BigDecimal("1.25");
    }

    /** Коэффициент франшизы Кф. */
    public static BigDecimal franchiseFactor(int percent) {
        switch (percent) {
            case 0: return new BigDecimal("1.00");
            case 1: return new BigDecimal("0.95");
            case 2: return new BigDecimal("0.90");
            case 5: return new BigDecimal("0.80");
            default: throw new IllegalArgumentException("Допустимая франшиза: 0, 1, 2 или 5 %");
        }
    }

    public static BigDecimal franchiseAmount(BigDecimal sum, int percent) {
        return Money.round(sum.multiply(new BigDecimal(percent)).divide(Money.HUNDRED, MC));
    }
}
