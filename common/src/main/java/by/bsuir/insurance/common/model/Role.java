package by.bsuir.insurance.common.model;

/** Роли пользователей системы. */
public enum Role {
    CLIENT("Клиент"),
    AGENT("Страховой агент"),
    ADMIN("Администратор");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
