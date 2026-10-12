package by.bsuir.insurance.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

/** Строка анализа убыточности вида страхования за период. */
public class LossRatioRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String typeName;
    private final BigDecimal premiums;
    private final BigDecimal payouts;
    /** Коэффициент убыточности, % (null, если премий не было). */
    private final BigDecimal lossRatio;

    public LossRatioRow(String typeName, BigDecimal premiums, BigDecimal payouts, BigDecimal lossRatio) {
        this.typeName = typeName;
        this.premiums = premiums;
        this.payouts = payouts;
        this.lossRatio = lossRatio;
    }

    public String getTypeName() { return typeName; }
    public BigDecimal getPremiums() { return premiums; }
    public BigDecimal getPayouts() { return payouts; }
    public BigDecimal getLossRatio() { return lossRatio; }
}
