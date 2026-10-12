package by.bsuir.insurance.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** Расчёт возврата части премии при досрочном расторжении договора. */
public class RefundCalculation implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal paidPremium;
    private long totalDays;
    private long remainingDays;
    private BigDecimal expensesShare;
    private BigDecimal payouts;
    private BigDecimal refund;

    public RefundCalculation(BigDecimal paidPremium, long totalDays, long remainingDays,
                             BigDecimal expensesShare, BigDecimal payouts, BigDecimal refund) {
        this.paidPremium = paidPremium;
        this.totalDays = totalDays;
        this.remainingDays = remainingDays;
        this.expensesShare = expensesShare;
        this.payouts = payouts;
        this.refund = refund;
    }

    public BigDecimal getPaidPremium() { return paidPremium; }
    public long getTotalDays() { return totalDays; }
    public long getRemainingDays() { return remainingDays; }
    public BigDecimal getExpensesShare() { return expensesShare; }
    public BigDecimal getPayouts() { return payouts; }
    public BigDecimal getRefund() { return refund; }
}
