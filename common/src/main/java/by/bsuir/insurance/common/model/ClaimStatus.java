package by.bsuir.insurance.common.model;

/** Статусы страхового случая. */
public enum ClaimStatus {
    REPORTED("Заявлен"),
    APPROVED("Выплата произведена"),
    REJECTED("Отказано");

    private final String title;

    ClaimStatus(String title) {
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
