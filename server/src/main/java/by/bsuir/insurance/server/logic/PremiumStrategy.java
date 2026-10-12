package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Client;

import java.math.BigDecimal;

/**
 * Стратегия расчёта поправочного коэффициента, специфичного для вида страхования
 * (паттерн «Стратегия»).
 */
public interface PremiumStrategy {

    /** Коэффициент вида страхования для заявки и страхователя. */
    BigDecimal typeFactor(Application application, Client client, int ageAtStart);

    /** Пояснение к коэффициенту для показа пользователю. */
    String factorName();
}
