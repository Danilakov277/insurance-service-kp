package by.bsuir.insurance.client.ui.components;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Универсальная модель таблицы для списка объектов (расширение класса JDK AbstractTableModel).
 *
 * @param <T> тип строк
 */
public class EntityTableModel<T> extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private final List<Column<T>> columns;
    private List<T> rows = new ArrayList<>();

    @SafeVarargs
    public EntityTableModel(Column<T>... columns) {
        this.columns = Arrays.asList(columns);
    }

    public void setRows(List<T> rows) {
        this.rows = rows == null ? new ArrayList<T>() : rows;
        fireTableDataChanged();
    }

    public List<T> getRows() {
        return rows;
    }

    public T getRow(int index) {
        return index < 0 || index >= rows.size() ? null : rows.get(index);
    }

    List<Column<T>> getColumns() {
        return columns;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).title;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Object v = columns.get(column).getter.apply(rows.get(row));
        return v == null ? "" : v;
    }
}
