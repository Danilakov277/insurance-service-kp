package by.bsuir.insurance.common.model;

import java.math.BigDecimal;

/** Вид страхования и его тарифы. */
public class InsuranceType extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private InsuranceCategory category;
    private String description;
    /** Базовый годовой тариф — доля страховой суммы. */
    private BigDecimal baseRate;
    private BigDecimal minSum;
    private BigDecimal maxSum;
    private int maxTermMonths;
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public InsuranceCategory getCategory() { return category; }
    public void setCategory(InsuranceCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    public BigDecimal getMinSum() { return minSum; }
    public void setMinSum(BigDecimal minSum) { this.minSum = minSum; }
    public BigDecimal getMaxSum() { return maxSum; }
    public void setMaxSum(BigDecimal maxSum) { this.maxSum = maxSum; }
    public int getMaxTermMonths() { return maxTermMonths; }
    public void setMaxTermMonths(int maxTermMonths) { this.maxTermMonths = maxTermMonths; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return name;
    }
}
