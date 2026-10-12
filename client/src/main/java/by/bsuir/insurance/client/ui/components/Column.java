package by.bsuir.insurance.client.ui.components;

import java.util.function.Function;

/** Описание столбца таблицы: заголовок, ширина и функция получения значения. */
public class Column<T> {
    final String title;
    final int width;
    final Function<T, Object> getter;

    public Column(String title, int width, Function<T, Object> getter) {
        this.title = title;
        this.width = width;
        this.getter = getter;
    }
}
