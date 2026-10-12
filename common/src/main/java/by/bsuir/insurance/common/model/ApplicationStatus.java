package by.bsuir.insurance.common.model;

/** Статусы заявки на страхование. */
public enum ApplicationStatus {
    NEW("На рассмотрении"),
    APPROVED("Одобрена"),
    REJECTED("Отклонена");

    private final String title;

    ApplicationStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
