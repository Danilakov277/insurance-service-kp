package by.bsuir.insurance.client.ui.client;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.client.ui.components.Form;
import by.bsuir.insurance.common.model.Claim;
import by.bsuir.insurance.common.model.Policy;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Страховые случаи клиента и подача заявления (UC7). */
public class MyClaimsPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<Claim> table = new EntityTable<>(
            new Column<Claim>("№", 40, Claim::getId),
            new Column<Claim>("Полис", 125, Claim::getPolicyNumber),
            new Column<Claim>("Дата события", 100, c -> Ui.format(c.getIncidentDate())),
            new Column<Claim>("Описание", 260, Claim::getDescription),
            new Column<Claim>("Ущерб, BYN", 100, Claim::getDamageAmount),
            new Column<Claim>("Статус", 140, Claim::getStatus),
            new Column<Claim>("Выплата, BYN", 100, Claim::getPayoutAmount),
            new Column<Claim>("Решение", 280, Claim::getDecisionComment));
    private final JComboBox<Policy> policy = new JComboBox<>();
    private final JTextField date = new JTextField(Ui.format(LocalDate.now()), 10);
    private final JTextField damage = new JTextField(10);
    private final JTextArea description = new JTextArea(3, 30);

    public MyClaimsPanel(Component owner) {
        this.owner = owner;
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        Form form = new Form();
        form.add("Договор:", policy).add("Дата события:", date).add("Размер ущерба, BYN:", damage)
                .add("Описание события:", new JScrollPane(description));
        JButton send = new JButton("Заявить о страховом случае");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(send);
        form.addWide(b);
        Ui.titled(form, "Новое заявление");
        add(Ui.title("Мои страховые случаи"), BorderLayout.NORTH);
        add(table.scroll(900, 300), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);
        send.addActionListener(e -> report());
    }

    @Override
    public void refresh() {
        List<Claim> list = Ui.call(owner, Ui.req(CommandType.GET_MY_CLAIMS));
        if (list != null) {
            table.setRows(list);
        }
        List<Policy> pols = Ui.call(owner, Ui.req(CommandType.GET_MY_POLICIES));
        if (pols != null) {
            List<Policy> allowed = new ArrayList<>();
            for (Policy p : pols) {
                if (p.getStatus().allowsClaims()) {
                    allowed.add(p);
                }
            }
            policy.setModel(new DefaultComboBoxModel<>(allowed.toArray(new Policy[0])));
        }
    }

    private void report() {
        try {
            Policy p = (Policy) policy.getSelectedItem();
            if (p == null) {
                throw new IllegalArgumentException("Нет договора, по которому можно заявить о страховом случае");
            }
            Claim c = new Claim();
            c.setPolicyId(p.getId());
            c.setIncidentDate(Ui.parseDate(date.getText(), "Дата события"));
            c.setDamageAmount(Ui.parseMoney(damage.getText(), "Размер ущерба"));
            c.setDescription(description.getText());
            if (Ui.callWithMessage(owner, Ui.req(CommandType.REPORT_CLAIM).with("claim", c)) != null) {
                description.setText("");
                damage.setText("");
                refresh();
            }
        } catch (IllegalArgumentException ex) {
            Ui.error(owner, ex.getMessage());
        }
    }
}
