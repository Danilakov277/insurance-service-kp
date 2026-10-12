package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Client;

import java.math.BigDecimal;

/** КАСКО: надбавка для молодых водителей. */
public class CascoPremiumStrategy extends AbstractPremiumStrategy {

    @Override
    protected BigDecimal extraFactor(Application application, Client client, int ageAtStart) {
        return ageAtStart < 25 ? new BigDecimal("1.15") : BigDecimal.ONE;
    }

    @Override
    public String factorName() {
        return "Кт (стаж водителя)";
    }
}
