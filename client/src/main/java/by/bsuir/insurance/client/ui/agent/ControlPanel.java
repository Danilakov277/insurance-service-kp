package by.bsuir.insurance.client.ui.agent;

import by.bsuir.insurance.client.ui.RefreshablePanel;
import by.bsuir.insurance.client.ui.Ui;
import by.bsuir.insurance.client.ui.components.Column;
import by.bsuir.insurance.client.ui.components.EntityTable;
import by.bsuir.insurance.common.model.ControlItem;
import by.bsuir.insurance.common.protocol.CommandType;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

/** Контроль просроченных взносов и истекающих договоров (UC11). */
public class ControlPanel extends RefreshablePanel {
    private static final long serialVersionUID = 1L;

    private final Component owner;
    private final EntityTable<ControlItem> table = new EntityTable<>(
            new Column<ControlItem>("Событие", 170, i -> i.getKind() == ControlItem.Kind.OVERDUE_PAYMENT
                    ? "Просрочен взнос" : "Истекает договор"),
            new Column<ControlItem>("Полис", 130, ControlItem::getPolicyNumber),
            new Column<ControlItem>("Клиент", 150, ControlItem::getClientName),
            new Column<ControlItem>("Телефон", 140, ControlItem::getClientPhone),
            new Column<ControlItem>("Дата", 100, i -> Ui.format(i.getDate())),
            new Column<ControlItem>("Сумма, BYN", 100, ControlItem::getAmount),
            new Column<ControlItem>("Дней", 60, i -> i.getKind() == ControlItem.Kind.OVERDUE_PAYMENT
                    ? "+" + i.getDays() : String.valueOf(i.getDays())));

    public ControlPanel(Component owner) {
        this.owner = owner;
        JButton recalc = new JButton("Пересчитать состояния договоров");
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        b.add(recalc);
        add(Ui.title("Просроченные взносы и договоры, истекающие в течение 30 дней"), BorderLayout.NORTH);
        add(table.scroll(900, 420), BorderLayout.CENTER);
        add(b, BorderLayout.SOUTH);
        recalc.addActionListener(e -> {
            if (Ui.callWithMessage(owner, Ui.req(CommandType.REFRESH_STATUSES)) != null) {
                refresh();
            }
        });
    }

    @Override
    public void refresh() {
        List<ControlItem> list = Ui.call(owner, Ui.req(CommandType.GET_CONTROL_LIST));
        if (list != null) {
            table.setRows(list);
        }
    }
}
