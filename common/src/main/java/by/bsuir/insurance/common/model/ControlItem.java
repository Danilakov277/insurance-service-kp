package by.bsuir.insurance.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Строка списка контроля: просроченный взнос или истекающий договор. */
public class ControlItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind { OVERDUE_PAYMENT, EXPIRING_POLICY }

    private final Kind kind;
    private final long policyId;
    private final String policyNumber;
    private final String clientName;
    private final String clientPhone;
    private final LocalDate date;
    private final BigDecimal amount;
    private final long days;

    public ControlItem(Kind kind, long policyId, String policyNumber, String clientName, String clientPhone,
                       LocalDate date, BigDecimal amount, long days) {
        this.kind = kind;
        this.policyId = policyId;
        this.policyNumber = policyNumber;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.date = date;
        this.amount = amount;
        this.days = days;
    }

    public Kind getKind() { return kind; }
    public long getPolicyId() { return policyId; }
    public String getPolicyNumber() { return policyNumber; }
    public String getClientName() { return clientName; }
    public String getClientPhone() { return clientPhone; }
    /** Срок уплаты взноса или дата окончания договора. */
    public LocalDate getDate() { return date; }
    public BigDecimal getAmount() { return amount; }
    /** Дней просрочки или дней до окончания. */
    public long getDays() { return days; }
}
