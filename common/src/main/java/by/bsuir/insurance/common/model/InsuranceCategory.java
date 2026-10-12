package by.bsuir.insurance.common.model;

/** Категория вида страхования: определяет стратегию расчёта премии. */
public enum InsuranceCategory {
    PROPERTY("Имущество"),
    CASCO("КАСКО"),
    ACCIDENT("Несчастные случаи"),
    TRAVEL("Выезжающие за рубеж");

    private final String title;

    InsuranceCategory(String title) {
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
