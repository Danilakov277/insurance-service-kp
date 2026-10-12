package by.bsuir.insurance.common.exception;

/** Нарушение бизнес-правила (например, заявка уже рассмотрена). */
public class BusinessRuleException extends InsuranceException {
    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
