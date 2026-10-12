package by.bsuir.insurance.server.logic;

import by.bsuir.insurance.common.exception.ValidationException;
import by.bsuir.insurance.common.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Формирование графика страховых взносов. */
public final class InstallmentScheduler {

    private InstallmentScheduler() {
    }

    public static void validate(int termMonths, int installments) throws ValidationException {
        if (installments != 1 && installments != 2 && installments != 4 && installments != 12) {
            throw new ValidationException("Число взносов может быть 1, 2, 4 или 12");
        }
        if (installments > termMonths || termMonths % installments != 0) {
            throw new ValidationException("Срок " + termMonths + " мес. нельзя разделить на " + installments + " равных взносов");
        }
    }

    /**
     * Делит премию на равные взносы; копейки, оставшиеся от деления, добавляются к последнему взносу.
     * Первый взнос — в дату начала договора, следующие — через равные интервалы.
     */
    public static List<Payment> build(long policyId, BigDecimal premium, int installments,
                                      LocalDate startDate, int termMonths) throws ValidationException {
        validate(termMonths, installments);
        int stepMonths = termMonths / installments;
        BigDecimal base = premium.divide(new BigDecimal(installments), 2, RoundingMode.DOWN);
        List<Payment> list = new ArrayList<>();
        for (int i = 1; i <= installments; i++) {
            BigDecimal amount = i < installments
                    ? base : premium.subtract(base.multiply(new BigDecimal(installments - 1)));
            list.add(new Payment(policyId, i, amount, startDate.plusMonths((long) stepMonths * (i - 1))));
        }
        return list;
    }
}
