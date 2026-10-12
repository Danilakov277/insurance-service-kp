package by.bsuir.insurance.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Результат расчёта страховой премии с расшифровкой коэффициентов. */
public class PremiumQuote implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal insuredSum;
    private BigDecimal baseRate;
    private BigDecimal premium;
    private BigDecimal franchise;
    private final Map<String, BigDecimal> factors = new LinkedHashMap<>();

    public BigDecimal getInsuredSum() { return insuredSum; }
    public void setInsuredSum(BigDecimal insuredSum) { this.insuredSum = insuredSum; }
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public BigDecimal getFranchise() { return franchise; }
    public void setFranchise(BigDecimal franchise) { this.franchise = franchise; }

    /** Поправочные коэффициенты в порядке применения: название → значение. */
    public Map<String, BigDecimal> getFactors() { return factors; }

    public void addFactor(String name, BigDecimal value) {
        factors.put(name, value);
    }
}
