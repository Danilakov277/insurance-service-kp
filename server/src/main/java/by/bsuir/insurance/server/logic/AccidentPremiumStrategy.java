package by.bsuir.insurance.server.logic;

import java.math.BigDecimal;

/** Страхование от несчастных случаев: надбавка за занятия спортом. */
public class AccidentPremiumStrategy extends AbstractPremiumStrategy {

    @Override
    protected String[] riskKeywords() {
        return new String[] {"спорт"};
    }

    @Override
    protected BigDecimal riskSurcharge() {
        return new BigDecimal("1.30");
    }

    @Override
    public String factorName() {
        return "Кт (занятия спортом)";
    }
}
