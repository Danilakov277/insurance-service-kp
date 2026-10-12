package by.bsuir.insurance.client.ui.admin;

import by.bsuir.insurance.client.ui.Ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.time.LocalDate;

/** Панель выбора периода отчёта с кнопками «Сформировать» и «Выгрузить в файл». */
class PeriodBar extends JPanel {
    private static final long serialVersionUID = 1L;

    final JTextField from = new JTextField(Ui.format(LocalDate.now().withDayOfYear(1)), 9);
    final JTextField to = new JTextField(Ui.format(LocalDate.now()), 9);
    final JButton build = new JButton("Сформировать");
    final JButton export = new JButton("Выгрузить в файл");

    PeriodBar(String title) {
        super(new FlowLayout(FlowLayout.LEFT));
        add(Ui.title(title));
        add(new JLabel("    Период с"));
        add(from);
        add(new JLabel("по"));
        add(to);
        add(build);
        add(export);
    }

    LocalDate getFrom() {
        return Ui.parseDate(from.getText(), "Начало периода");
    }

    LocalDate getTo() {
        return Ui.parseDate(to.getText(), "Окончание периода");
    }

    String periodText() {
        return from.getText() + " – " + to.getText();
    }
}
