package by.bsuir.insurance.client.ui;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Рабочая панель вкладки, умеющая перечитывать данные с сервера. */
public abstract class RefreshablePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    protected RefreshablePanel() {
        super(new BorderLayout(8, 8));
        setBorder(Ui.padding(10));
    }

    /** Загрузить актуальные данные с сервера. */
    public abstract void refresh();
}
