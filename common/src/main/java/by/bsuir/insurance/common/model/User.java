package by.bsuir.insurance.common.model;

import java.time.LocalDateTime;

/** Учётная запись пользователя. Пароль на клиент никогда не передаётся. */
public class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String login;
    private Role role;
    private boolean blocked;
    private LocalDateTime createdAt;
    /** Отображаемое имя (ФИО клиента или агента, для администратора — логин). */
    private String displayName;
    /** Идентификатор клиента или агента, связанного с учётной записью. */
    private long personId;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public long getPersonId() { return personId; }
    public void setPersonId(long personId) { this.personId = personId; }

    @Override
    public String toString() {
        return login + " (" + role.getTitle() + ")";
    }
}
