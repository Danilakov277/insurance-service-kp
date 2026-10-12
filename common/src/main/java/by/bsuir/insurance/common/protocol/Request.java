package by.bsuir.insurance.common.protocol;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Запрос клиента к серверу: команда и именованные параметры. */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private final CommandType command;
    private final Map<String, Serializable> params = new HashMap<>();

    public Request(CommandType command) {
        this.command = command;
    }

    public CommandType getCommand() {
        return command;
    }

    /** Добавить параметр (цепочкой вызовов). */
    public Request with(String name, Serializable value) {
        params.put(name, value);
        return this;
    }

    public Serializable get(String name) {
        return params.get(name);
    }

    public boolean has(String name) {
        return params.get(name) != null;
    }

    public String getString(String name) {
        Object v = params.get(name);
        return v == null ? null : v.toString().trim();
    }

    public long getLong(String name) {
        Object v = params.get(name);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    public int getInt(String name) {
        return (int) getLong(name);
    }

    public BigDecimal getDecimal(String name) {
        Object v = params.get(name);
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(String.valueOf(v).replace(',', '.').trim());
    }

    public LocalDate getDate(String name) {
        Object v = params.get(name);
        if (v == null) {
            return null;
        }
        return v instanceof LocalDate ? (LocalDate) v : LocalDate.parse(String.valueOf(v));
    }

    public boolean getBoolean(String name) {
        Object v = params.get(name);
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v));
    }

    @Override
    public String toString() {
        return command + params.keySet().toString();
    }
}
