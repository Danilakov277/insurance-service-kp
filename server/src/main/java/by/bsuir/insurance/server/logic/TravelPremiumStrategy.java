package by.bsuir.insurance.server.logic;

import java.math.BigDecimal;

/** Медицинские расходы выезжающих за рубеж: надбавка за активный отдых. */
public class TravelPremiumStrategy extends AbstractPremiumStrategy {

    @Override
    protected String[] riskKeywords() {
        return new String[] {"горнолыж", "спорт"};
    }

    @Override
    protected BigDecimal riskSurcharge() {
        return new BigDecimal("1.50");
    }

    @Override
    public String factorName() {
        return "Кт (активный отдых)";
    }
}
