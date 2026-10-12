package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.Application;
import by.bsuir.insurance.common.model.Client;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Базовая стратегия: коэффициент равен 1, если описание объекта не содержит
 * ключевых слов повышенного риска; наследники задают свои ключевые слова и надбавки.
 */
public abstract class AbstractPremiumStrategy implements PremiumStrategy {

    @Override
    public BigDecimal typeFactor(Application application, Client client, int ageAtStart) {
        BigDecimal factor = extraFactor(application, client, ageAtStart);
        String description = application.getObjectDescription() == null
                ? "" : application.getObjectDescription().toLowerCase(Locale.ROOT);
        for (String keyword : riskKeywords()) {
            if (description.contains(keyword)) {
                factor = factor.multiply(riskSurcharge());
                break;
            }
        }
        return factor;
    }

    /** Дополнительный множитель, зависящий от параметров договора (переопределяется). */
    protected BigDecimal extraFactor(Application application, Client client, int ageAtStart) {
        return BigDecimal.ONE;
    }

    /** Ключевые слова в описании объекта, означающие повышенный риск. */
    protected String[] riskKeywords() {
        return new String[0];
    }

    /** Надбавка за повышенный риск. */
    protected BigDecimal riskSurcharge() {
        return BigDecimal.ONE;
    }
}
