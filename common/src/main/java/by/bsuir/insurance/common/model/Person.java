package by.bsuir.insurance.common.model;

/**
 * Физическое лицо: общий предок клиента и страхового агента.
 */
public abstract class Person extends Entity {
    private static final long serialVersionUID = 1L;

    private long userId;
    private String lastName;
    private String firstName;
    private String patronymic;

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }

    /** Фамилия, имя и отчество полностью. */
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(lastName).append(' ').append(firstName);
        if (patronymic != null && !patronymic.isEmpty()) {
            sb.append(' ').append(patronymic);
        }
        return sb.toString();
    }

    /** Фамилия и инициалы, например «Иванов И.И.». */
    public String getShortName() {
        StringBuilder sb = new StringBuilder(lastName).append(' ');
        if (firstName != null && !firstName.isEmpty()) {
            sb.append(firstName.charAt(0)).append('.');
        }
        if (patronymic != null && !patronymic.isEmpty()) {
            sb.append(patronymic.charAt(0)).append('.');
        }
        return sb.toString();
    }

    /** Роль лица в системе (переопределяется наследниками). */
    public abstract Role getRole();

    @Override
    public String toString() {
        return getShortName();
    }
}
