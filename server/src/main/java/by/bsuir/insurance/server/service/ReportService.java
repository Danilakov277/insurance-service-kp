package by.bsuir.insurance.server.service;

import by.bsuir.insurance.common.model.LossRatioRow;
import by.bsuir.insurance.common.model.SalesReportRow;
import by.bsuir.insurance.server.dao.ReportDao;
import by.bsuir.insurance.server.db.ConnectionManager;

import java.time.LocalDate;
import java.util.ArrayList;

/** Отчёты и аналитика для администратора (UC15, UC16). */
public class ReportService {
    private final ReportDao dao = new ReportDao();
    private final ConnectionManager cm = ConnectionManager.getInstance();

    public ArrayList<SalesReportRow> sales(LocalDate from, LocalDate to) throws Exception {
        Validator.period(from, to);
        return new ArrayList<>(cm.withConnection(c -> dao.sales(c, from, to)));
    }

    public ArrayList<LossRatioRow> lossRatio(LocalDate from, LocalDate to) throws Exception {
        Validator.period(from, to);
        return new ArrayList<>(cm.withConnection(c -> dao.lossRatio(c, from, to)));
    }
}
