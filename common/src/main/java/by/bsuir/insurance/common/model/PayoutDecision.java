package by.bsuir.insurance.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Решение по страховому случаю, сформированное алгоритмом расчёта выплаты. */
public class PayoutDecision implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean approved;
    private BigDecimal amount = BigDecimal.ZERO;
    private String reason;
    private boolean exhaustsSum;
    private final List<String> steps = new ArrayList<>();

    public static PayoutDecision reject(String reason, List<String> steps) {
        PayoutDecision d = new PayoutDecision();
        d.approved = false;
        d.reason = reason;
        d.steps.addAll(steps);
        d.steps.add("Отказ: " + reason);
        return d;
    }

    public static PayoutDecision approve(BigDecimal amount, boolean exhaustsSum, List<String> steps) {
        PayoutDecision d = new PayoutDecision();
        d.approved = true;
        d.amount = amount;
        d.exhaustsSum = exhaustsSum;
        d.reason = "Выплата " + amount + " BYN";
        d.steps.addAll(steps);
        return d;
    }

    public boolean isApproved() { return approved; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public boolean isExhaustsSum() { return exhaustsSum; }
    /** Пошаговое обоснование решения (для показа агенту). */
    public List<String> getSteps() { return steps; }
}
