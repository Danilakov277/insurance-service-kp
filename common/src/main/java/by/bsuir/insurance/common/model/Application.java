package by.bsuir.insurance.common.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Заявка клиента на заключение договора страхования. */
public class Application extends Entity {
    private static final long serialVersionUID = 1L;

    private long clientId;
    private String clientName;
    private long typeId;
    private String typeName;
    private Long agentId;
    private String agentName;
    private BigDecimal requestedSum;
    private int termMonths;
    private int franchisePercent;
    private int installments;
    private String objectDescription;
    private ApplicationStatus status = ApplicationStatus.NEW;
    private String decisionComment;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;
    /** Предварительно рассчитанная премия (заполняется сервером при выдаче списка). */
    private BigDecimal estimatedPremium;

    public long getClientId() { return clientId; }
    public void setClientId(long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public long getTypeId() { return typeId; }
    public void setTypeId(long typeId) { this.typeId = typeId; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public BigDecimal getRequestedSum() { return requestedSum; }
    public void setRequestedSum(BigDecimal requestedSum) { this.requestedSum = requestedSum; }
    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }
    public int getFranchisePercent() { return franchisePercent; }
    public void setFranchisePercent(int franchisePercent) { this.franchisePercent = franchisePercent; }
    public int getInstallments() { return installments; }
    public void setInstallments(int installments) { this.installments = installments; }
    public String getObjectDescription() { return objectDescription; }
    public void setObjectDescription(String objectDescription) { this.objectDescription = objectDescription; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String decisionComment) { this.decisionComment = decisionComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public BigDecimal getEstimatedPremium() { return estimatedPremium; }
    public void setEstimatedPremium(BigDecimal estimatedPremium) { this.estimatedPremium = estimatedPremium; }
}
