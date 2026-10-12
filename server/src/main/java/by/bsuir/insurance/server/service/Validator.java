package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.exception.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** Проверка входных данных запросов. */
final class Validator {
    private static final Pattern LOGIN = Pattern.compile("[A-Za-z0-9_.]{3,50}");
    private static final Pattern NAME = Pattern.compile("[А-Яа-яЁёA-Za-z' -]{1,50}");
    private static final Pattern PASSPORT = Pattern.compile("[A-Za-z]{2}\\d{7}");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9 ()-]{7,20}");

    private Validator() {
    }

    static String required(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Поле «" + field + "» не заполнено");
        }
        return value.trim();
    }

    static String login(String value) throws ValidationException {
        String v = required(value, "Логин");
        if (!LOGIN.matcher(v).matches()) {
            throw new ValidationException("Логин: от 3 до 50 латинских букв, цифр, «_» или «.»");
        }
        return v;
    }

    static String password(String value) throws ValidationException {
        if (value == null || value.length() < 6) {
            throw new ValidationException("Пароль должен содержать не менее 6 символов");
        }
        return value;
    }

    static String name(String value, String field, boolean mandatory) throws ValidationException {
        if (!mandatory && (value == null || value.trim().isEmpty())) {
            return null;
        }
        String v = required(value, field);
        if (!NAME.matcher(v).matches()) {
            throw new ValidationException("Поле «" + field + "» может содержать только буквы, пробел и дефис");
        }
        return v;
    }

    static String passport(String value) throws ValidationException {
        String v = required(value, "Номер паспорта").replace(" ", "");
        if (!PASSPORT.matcher(v).matches()) {
            throw new ValidationException("Номер паспорта: две буквы серии и семь цифр, например MP1234567");
        }
        return v.toUpperCase();
    }

    static String phone(String value) throws ValidationException {
        String v = required(value, "Телефон");
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException("Телефон указан неверно");
        }
        return v;
    }

    static BigDecimal positive(BigDecimal value, String field) throws ValidationException {
        if (value == null || value.signum() <= 0) {
            throw new ValidationException("Поле «" + field + "» должно быть положительным числом");
        }
        return value;
    }

    static LocalDate notFuture(LocalDate date, String field) throws ValidationException {
        if (date == null) {
            throw new ValidationException("Поле «" + field + "» не заполнено");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Дата «" + field + "» не может быть в будущем");
        }
        return date;
    }

    static void period(LocalDate from, LocalDate to) throws ValidationException {
        if (from == null || to == null || from.isAfter(to)) {
            throw new ValidationException("Начало периода должно быть не позже его окончания");
        }
    }
}
