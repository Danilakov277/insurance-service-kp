package by.bsuir.insurance.common.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Страховой агент — сотрудник страховой компании. */
public class Agent extends Person {
    private static final long serialVersionUID = 1L;

    private String position;
    private LocalDate hireDate;
    private BigDecimal commissionRate;
    private String login;
    private boolean blocked;

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    @Override
    public Role getRole() {
        return Role.AGENT;
    }
}
