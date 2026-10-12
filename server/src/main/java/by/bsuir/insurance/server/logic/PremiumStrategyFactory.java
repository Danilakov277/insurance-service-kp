package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.model.InsuranceCategory;
import by.bsuir.insurance.common.model.InsuranceType;

import java.util.EnumMap;
import java.util.Map;

/** Фабрика стратегий расчёта премии (паттерн «Фабрика»). */
public final class PremiumStrategyFactory {
    private static final Map<InsuranceCategory, PremiumStrategy> STRATEGIES = new EnumMap<>(InsuranceCategory.class);

    static {
        STRATEGIES.put(InsuranceCategory.PROPERTY, new PropertyPremiumStrategy());
        STRATEGIES.put(InsuranceCategory.CASCO, new CascoPremiumStrategy());
        STRATEGIES.put(InsuranceCategory.ACCIDENT, new AccidentPremiumStrategy());
        STRATEGIES.put(InsuranceCategory.TRAVEL, new TravelPremiumStrategy());
    }

    private PremiumStrategyFactory() {
    }

    public static PremiumStrategy forType(InsuranceType type) {
        PremiumStrategy strategy = STRATEGIES.get(type.getCategory());
        if (strategy == null) {
            throw new IllegalArgumentException("Нет стратегии для категории " + type.getCategory());
        }
        return strategy;
    }
}
