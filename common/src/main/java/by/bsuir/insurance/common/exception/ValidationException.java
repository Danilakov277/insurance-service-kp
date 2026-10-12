package by.bsuir.insurance.common.exception;

/** Некорректные входные данные. */
public class ValidationException extends InsuranceException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
