package by.bsuir.insurance.common.exception;

/** Базовое проверяемое исключение предметной области. */
public class InsuranceException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsuranceException(String message) {
        super(message);
    }

    public InsuranceException(String message, Throwable cause) {
        super(message, cause);
    }
}
