package by.bsuir.insurance.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** Строка отчёта о продажах агента за период. */
public class SalesReportRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String agentName;
    private final int policiesCount;
    private final BigDecimal premiumSum;
    private final BigDecimal commission;

    public SalesReportRow(String agentName, int policiesCount, BigDecimal premiumSum, BigDecimal commission) {
        this.agentName = agentName;
        this.policiesCount = policiesCount;
        this.premiumSum = premiumSum;
        this.commission = commission;
    }

    public String getAgentName() { return agentName; }
    public int getPoliciesCount() { return policiesCount; }
    public BigDecimal getPremiumSum() { return premiumSum; }
    public BigDecimal getCommission() { return commission; }
}
