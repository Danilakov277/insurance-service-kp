package by.bsuir.insurance.common.model;

import java.io.Serializable;

/**
 * Базовый класс всех сущностей предметной области.
 * Сущности передаются между клиентом и сервером в сериализованном виде.
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isNew() {
        return id == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity other = (Entity) o;
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
