package by.bsuir.insurance.client.ui.components;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Панель формы «подпись — поле» на GridBagLayout. */
public class Form extends JPanel {
    private static final long serialVersionUID = 1L;
    private int row;

    public Form() {
        super(new GridBagLayout());
    }

    public Form add(String label, JComponent field) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(4, 4, 4, 10);
        add(new JLabel(label), l);
        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1;
        f.gridy = row++;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(4, 0, 4, 4);
        add(field, f);
        return this;
    }

    /** Компонент на всю ширину формы. */
    public Form addWide(JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row++;
        g.gridwidth = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 4, 4, 4);
        add(c, g);
        return this;
    }

    /** Заполнитель, прижимающий поля к верху. */
    public Form fillRest() {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = row++;
        g.weighty = 1;
        add(new JPanel(), g);
        return this;
    }
}
