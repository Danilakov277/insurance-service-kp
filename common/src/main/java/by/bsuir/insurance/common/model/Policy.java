package by.bsuir.insurance.common.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Договор страхования (полис). */
public class Policy extends Entity {
    private static final long serialVersionUID = 1L;

    private String policyNumber;
    private Long applicationId;
    private long clientId;
    private String clientName;
    private long agentId;
    private String agentName;
    private long typeId;
    private String typeName;
    private BigDecimal insuredSum;
    private BigDecimal premium;
    private BigDecimal franchise;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime terminatedAt;
    private BigDecimal refundAmount;
    /** Сумма уже уплаченных взносов (вычисляемое поле). */
    private BigDecimal paidAmount;
    /** Сумма произведённых выплат (вычисляемое поле). */
    private BigDecimal payoutsAmount;

    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public long getAgentId() { return agentId; }
    public void setAgentId(long agentId) { this.agentId = agentId; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public long getTypeId() { return typeId; }
    public void setTypeId(long typeId) { this.typeId = typeId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public BigDecimal getInsuredSum() { return insuredSum; }
    public void setInsuredSum(BigDecimal insuredSum) { this.insuredSum = insuredSum; }
    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }
    public BigDecimal getFranchise() { return franchise; }
    public void setFranchise(BigDecimal franchise) { this.franchise = franchise; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(LocalDateTime terminatedAt) { this.terminatedAt = terminatedAt; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public BigDecimal getPayoutsAmount() { return payoutsAmount; }
    public void setPayoutsAmount(BigDecimal payoutsAmount) { this.payoutsAmount = payoutsAmount; }

    /** Действовал ли договор (по сроку) на указанную дату. */
    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public String toString() {
        return policyNumber + " (" + typeName + ")";
    }
}
