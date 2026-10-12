package by.bsuir.insurance.client.ui.client;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.InsuranceType;
import by.bsuir.insurance.common.model.PremiumQuote;
import by.bsuir.insurance.common.protocol.CommandType;
import by.bsuir.insurance.common.util.Money;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Расчёт стоимости страхования (UC3) и подача заявки (UC4). */
public class CalculatorPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;
    private static final int[] TERMS = {1, 3, 6, 12, 24, 36};

    private final Component owner;
    private final JComboBox<InsuranceType> type = new JComboBox<>();
    private final JLabel typeInfo = Ui.hint(" ");
    private final JTextField sum = new JTextField("50000", 12);
    private final JComboBox<Integer> term = new JComboBox<>();
    private final JComboBox<Integer> franchise = new JComboBox<>(new Integer[] {0, 1, 2, 5});
    private final JComboBox<Integer> installments = new JComboBox<>();
    private final JTextArea description = new JTextArea(4, 28);
    private final JLabel premium = new JLabel("—");
    private final JLabel details = Ui.hint(" ");
    private final EntityTable<Map.Entry<String, BigDecimal>> factors = new EntityTable<>(
            new Column<Map.Entry<String, BigDecimal>>("Коэффициент", 260, Map.Entry::getKey),
            new Column<Map.Entry<String, BigDecimal>>("Значение", 90, e -> e.getValue().setScale(4, RoundingMode.HALF_UP)
                    .toPlainString().replace('.', ',')));

    public CalculatorPanel(Component owner) {
        this.owner = owner;
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        Form form = new Form();
        form.add("Вид страхования:", type).addWide(typeInfo)
                .add("Страховая сумма, BYN:", sum).add("Срок, мес.:", term)
                .add("Франшиза, %:", franchise).add("Число взносов:", installments)
                .add("Объект страхования:", new JScrollPane(description));
        JButton calc = new JButton("Рассчитать стоимость");
        JButton submit = new JButton("Подать заявку");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(calc);
        buttons.add(submit);
        form.addWide(buttons).fillRest();
        Ui.titled(form, "Параметры страхования");

        premium.setFont(premium.getFont().deriveFont(Font.BOLD, 26f));
        premium.setForeground(Ui.ACCENT);
        JPanel result = new JPanel(new BorderLayout(6, 6));
        JPanel top = new JPanel(new GridLayout(3, 1));
        top.add(new JLabel("Страховая премия, BYN:"));
        top.add(premium);
        top.add(details);
        result.add(top, BorderLayout.NORTH);
        result.add(factors.scroll(380, 200), BorderLayout.CENTER);
        result.add(Ui.hint("<html>П = С × Т × Кв × Кс × Кбм × Кф × Кт, где С — страховая сумма, "
                + "Т — базовый тариф вида страхования</html>"), BorderLayout.SOUTH);
        Ui.titled(result, "Результат расчёта");

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));
        center.add(form);
        center.add(result);
        add(Ui.title("Расчёт стоимости страхования"), BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        type.addActionListener(e -> onTypeChanged());
        term.addActionListener(e -> fillInstallments());
        calc.addActionListener(e -> calculate());
        submit.addActionListener(e -> submit());
    }

    @Override
    public void refresh() {
        List<InsuranceType> types = Ui.call(owner, Ui.req(CommandType.GET_ACTIVE_TYPES));
        if (types != null) {
            Object sel = type.getSelectedItem();
            type.setModel(new DefaultComboBoxModel<>(types.toArray(new InsuranceType[0])));
            if (sel != null) {
                type.setSelectedItem(sel);
            }
            onTypeChanged();
        }
    }

    private void onTypeChanged() {
        InsuranceType t = (InsuranceType) type.getSelectedItem();
        if (t == null) {
            return;
        }
        typeInfo.setText("<html>" + t.getDescription() + "<br>Сумма: " + Money.format(t.getMinSum()) + " – "
                + Money.format(t.getMaxSum()) + " BYN, срок до " + t.getMaxTermMonths() + " мес.</html>");
        List<Integer> terms = new ArrayList<>();
        for (int m : TERMS) {
            if (m <= t.getMaxTermMonths()) {
                terms.add(m);
            }
        }
        term.setModel(new DefaultComboBoxModel<>(terms.toArray(new Integer[0])));
        term.setSelectedItem(t.getMaxTermMonths() >= 12 ? 12 : terms.get(terms.size() - 1));
        fillInstallments();
    }

    private void fillInstallments() {
        Integer m = (Integer) term.getSelectedItem();
        if (m == null) {
            return;
        }
        List<Integer> opts = new ArrayList<>();
        for (int n : new int[] {1, 2, 4, 12}) {
            if (n <= m && m % n == 0) {
                opts.add(n);
            }
        }
        installments.setModel(new DefaultComboBoxModel<>(opts.toArray(new Integer[0])));
    }

    private Application params() {
        InsuranceType t = (InsuranceType) type.getSelectedItem();
        if (t == null) {
            throw new IllegalArgumentException("Выберите вид страхования");
        }
        Application a = new Application();
        a.setTypeId(t.getId());
        a.setRequestedSum(Ui.parseMoney(sum.getText(), "Страховая сумма"));
        a.setTermMonths((Integer) term.getSelectedItem());
        a.setFranchisePercent((Integer) franchise.getSelectedItem());
        a.setInstallments((Integer) installments.getSelectedItem());
        a.setObjectDescription(description.getText());
        return a;
    }

    private void calculate() {
        try {
            Application a = params();
            PremiumQuote q = Ui.call(owner, Ui.req(CommandType.CALCULATE_PREMIUM).with("application", a));
            if (q == null) {
                return;
            }
            premium.setText(Money.format(q.getPremium()));
            BigDecimal perInstallment = q.getPremium().divide(new BigDecimal(a.getInstallments()), 2, RoundingMode.DOWN);
            details.setText("Франшиза: " + Money.format(q.getFranchise()) + " BYN;  взнос: "
                    + Money.format(perInstallment) + " BYN × " + a.getInstallments());
            List<Map.Entry<String, BigDecimal>> rows = new ArrayList<>();
            rows.add(new java.util.AbstractMap.SimpleEntry<>("Т — базовый тариф", q.getBaseRate()));
            rows.addAll(q.getFactors().entrySet());
            factors.setRows(rows);
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }

    private void submit() {
        try {
            Application a = params();
            if (a.getObjectDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Опишите объект страхования");
            }
            if (Ui.callWithMessage(owner, Ui.req(CommandType.SUBMIT_APPLICATION).with("application", a)) != null) {
                description.setText("");
            }
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }
}
