package by.bsuir.insurance.common.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Страховой взнос по графику. Первичный ключ — (policyId, installmentNo),
 * поэтому поле id базового класса не используется.
 */
public class Payment extends Entity {
    private static final long serialVersionUID = 1L;

    private long policyId;
    private int installmentNo;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paidDate;

    public Payment() {
    }

    public Payment(long policyId, int installmentNo, BigDecimal amount, LocalDate dueDate) {
        this.policyId = policyId;
        this.installmentNo = installmentNo;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    public long getPolicyId() { return policyId; }
    public void setPolicyId(long policyId) { this.policyId = policyId; }
    public int getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(int installmentNo) { this.installmentNo = installmentNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate paidDate) { this.paidDate = paidDate; }

    public boolean isPaid() {
        return paidDate != null;
    }

    /** Просрочен ли взнос на указанную дату. */
    public boolean isOverdue(LocalDate on) {
        return dueDate.isBefore(on) && (paidDate == null || paidDate.isAfter(on));
    }
}
