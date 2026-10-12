package by.bsuir.insurance.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Вспомогательные операции с денежными суммами (BYN, до копеек). */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    public static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    /** Округление до копеек по правилам бухгалтерского учёта. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(String value) {
        return round(new BigDecimal(value));
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    /** Форматирование суммы для отображения: «12 345,67». */
    public static String format(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("ru", "BY"));
        s.setGroupingSeparator(' ');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", s).format(value);
    }
}
