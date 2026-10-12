package by.bsuir.insurance.common.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Страховой случай (заявление о наступлении события и решение по нему). */
public class Claim extends Entity {
    private static final long serialVersionUID = 1L;

    private long policyId;
    private String policyNumber;
    private String clientName;
    private String typeName;
    private LocalDate incidentDate;
    private LocalDate reportDate;
    private String description;
    private BigDecimal damageAmount;
    private BigDecimal assessedDamage;
    private ClaimStatus status = ClaimStatus.REPORTED;
    private BigDecimal payoutAmount;
    private Long agentId;
    private String agentName;
    private String decisionComment;
    private LocalDateTime decidedAt;

    public long getPolicyId() { return policyId; }
    public void setPolicyId(long policyId) { this.policyId = policyId; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate incidentDate) { this.incidentDate = incidentDate; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getDamageAmount() { return damageAmount; }
    public void setDamageAmount(BigDecimal damageAmount) { this.damageAmount = damageAmount; }
    public BigDecimal getAssessedDamage() { return assessedDamage; }
    public void setAssessedDamage(BigDecimal assessedDamage) { this.assessedDamage = assessedDamage; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }
    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public void setPayoutAmount(BigDecimal payoutAmount) { this.payoutAmount = payoutAmount; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String decisionComment) { this.decisionComment = decisionComment; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
