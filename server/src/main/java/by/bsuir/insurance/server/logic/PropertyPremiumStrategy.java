package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Client;

import java.math.BigDecimal;

/** Страхование имущества: скидка за крупную сумму, надбавка за деревянные строения. */
public class PropertyPremiumStrategy extends AbstractPremiumStrategy {
    private static final BigDecimal LARGE_SUM = new BigDecimal("100000");

    @Override
    protected BigDecimal extraFactor(Application application, Client client, int ageAtStart) {
        return application.getRequestedSum().compareTo(LARGE_SUM) > 0 ? new BigDecimal("0.95") : BigDecimal.ONE;
    }

    @Override
    protected String[] riskKeywords() {
        return new String[] {"деревян"};
    }

    @Override
    protected BigDecimal riskSurcharge() {
        return new BigDecimal("1.20");
    }

    @Override
    public String factorName() {
        return "Кт (сумма, материал стен)";
    }
}
