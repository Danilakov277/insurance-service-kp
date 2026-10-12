package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.InsuranceCategory;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;

/** Управление видами страхования и тарифами (UC13). */
public class TypesPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<InsuranceType> table = new EntityTable<>(
            new Column<InsuranceType>("№", 40, InsuranceType::getId),
            new Column<InsuranceType>("Наименование", 260, InsuranceType::getName),
            new Column<InsuranceType>("Категория", 140, InsuranceType::getCategory),
            new Column<InsuranceType>("Тариф", 70, t -> t.getBaseRate().toPlainString().replace('.', ',')),
            new Column<InsuranceType>("Мин. сумма", 100, InsuranceType::getMinSum),
            new Column<InsuranceType>("Макс. сумма", 100, InsuranceType::getMaxSum),
            new Column<InsuranceType>("Срок до, мес.", 90, InsuranceType::getMaxTermMonths),
            new Column<InsuranceType>("Активен", 70, t -> t.isActive() ? "да" : "нет"));
    private final JTextField name = new JTextField(28);
    private final JComboBox<InsuranceCategory> category = new JComboBox<>(InsuranceCategory.values());
    private final JTextField description = new JTextField(40);
    private final JTextField rate = new JTextField(8);
    private final JTextField minSum = new JTextField(10);
    private final JTextField maxSum = new JTextField(10);
    private final JTextField maxTerm = new JTextField(5);
    private final JCheckBox active = new JCheckBox("Доступен для новых договоров", true);
    private InsuranceType editing;

    public TypesPanel(Component owner) {
        this.owner = owner;
        Form form = new Form();
        form.add("Наименование:", name).add("Категория:", category).add("Описание:", description)
                .add("Базовый тариф (доля в год):", rate).add("Минимальная сумма, BYN:", minSum)
                .add("Максимальная сумма, BYN:", maxSum).add("Максимальный срок, мес.:", maxTerm).addWide(active);
        JButton create = new JButton("Новый вид");
        JButton save = new JButton("Сохранить");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(create);
        b.add(save);
        form.addWide(b);
        add(Ui.title("Виды страхования и тарифы"), BorderLayout.NORTH);
        add(table.scroll(900, 260), BorderLayout.CENTER);
        add(Ui.titled(form, "Карточка вида страхования"), BorderLayout.SOUTH);
        table.onSelect(this::edit);
        create.addActionListener(e -> {
            table.clearSelection();
            edit(null);
        });
        save.addActionListener(e -> save());
    }

    @Override
    public void refresh() {
        List<InsuranceType> list = Ui.call(owner, Ui.req(CommandType.GET_ALL_TYPES));
        if (list != null) {
            table.setRows(list);
        }
    }

    private void edit(InsuranceType t) {
        editing = t;
        name.setText(t == null ? "" : t.getName());
        category.setSelectedItem(t == null ? InsuranceCategory.PROPERTY : t.getCategory());
        description.setText(t == null ? "" : t.getDescription());
        rate.setText(t == null ? "" : t.getBaseRate().toPlainString());
        minSum.setText(t == null ? "" : t.getMinSum().toPlainString());
        maxSum.setText(t == null ? "" : t.getMaxSum().toPlainString());
        maxTerm.setText(t == null ? "12" : String.valueOf(t.getMaxTermMonths()));
        active.setSelected(t == null || t.isActive());
    }

    private void save() {
        try {
            InsuranceType t = new InsuranceType();
            t.setId(editing == null ? 0 : editing.getId());
            t.setName(name.getText());
            t.setCategory((InsuranceCategory) category.getSelectedItem());
            t.setDescription(description.getText());
            t.setBaseRate(new BigDecimal(rate.getText().trim().replace(',', '.')));
            t.setMinSum(Ui.parseMoney(minSum.getText(), "Минимальная сумма"));
            t.setMaxSum(Ui.parseMoney(maxSum.getText(), "Максимальная сумма"));
            t.setMaxTermMonths(Integer.parseInt(maxTerm.getText().trim()));
            t.setActive(active.isSelected());
            if (Ui.callWithMessage(owner, Ui.req(CommandType.SAVE_TYPE).with("type", t)) != null) {
                refresh();
            }
        } catch (NumberFormatException ex) {
            Ui.error(owner, "Тариф и срок должны быть числами");
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }
}
