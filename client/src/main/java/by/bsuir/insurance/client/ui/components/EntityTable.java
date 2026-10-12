package by.bsuir.insurance.client.ui.components;

import by.bsuir.insurance.common.util.Money;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/** Таблица объектов с выравниванием денежных сумм и выбором строки. */
public class EntityTable<T> extends JTable {
    private static final long serialVersionUID = 1L;

    private final EntityTableModel<T> model;

    @SafeVarargs
    public EntityTable(Column<T>... columns) {
        this(new EntityTableModel<>(columns));
    }

    public EntityTable(EntityTableModel<T> model) {
        super(model);
        this.model = model;
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setRowHeight(24);
        setFillsViewportHeight(true);
        setAutoCreateRowSorter(true);
        getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < model.getColumns().size(); i++) {
            int w = model.getColumns().get(i).width;
            if (w > 0) {
                getColumnModel().getColumn(i).setPreferredWidth(w);
            }
        }
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
                Object shown = v instanceof BigDecimal ? Money.format((BigDecimal) v) : v;
                Component comp = super.getTableCellRendererComponent(t, shown, sel, focus, r, c);
                setHorizontalAlignment(v instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
                return comp;
            }
        });
    }

    public void setRows(List<T> rows) {
        model.setRows(rows);
    }

    public List<T> getRows() {
        return model.getRows();
    }

    /** Выбранный объект (с учётом сортировки) или null. */
    public T getSelected() {
        int view = getSelectedRow();
        return view < 0 ? null : model.getRow(convertRowIndexToModel(view));
    }

    public void onSelect(Consumer<T> action) {
        getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                action.accept(getSelected());
            }
        });
    }

    public JScrollPane scroll(int width, int height) {
        JScrollPane sp = new JScrollPane(this);
        sp.setPreferredSize(new Dimension(width, height));
        return sp;
    }
}
