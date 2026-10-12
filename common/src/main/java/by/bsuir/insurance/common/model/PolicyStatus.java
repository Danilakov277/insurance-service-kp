package by.bsuir.insurance.common.model;

/** Состояния договора страхования (см. диаграмму состояний). */
public enum PolicyStatus {
    WAITING_PAYMENT("Ожидает оплаты"),
    ACTIVE("Действует"),
    SUSPENDED("Приостановлен"),
    EXPIRED("Истёк"),
    TERMINATED("Расторгнут"),
    FULFILLED("Исполнен");

    private final String title;

    PolicyStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Может ли договор служить основанием для заявления о страховом случае. */
    public boolean allowsClaims() {
        return this == ACTIVE || this == SUSPENDED || this == EXPIRED;
    }

    @Override
    public String toString() {
        return title;
    }
}
