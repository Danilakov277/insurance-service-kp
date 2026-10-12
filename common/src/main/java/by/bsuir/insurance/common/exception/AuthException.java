package by.bsuir.insurance.common.exception;

/** Ошибка авторизации или недостаточно прав. */
public class AuthException extends InsuranceException {
    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super(message);
    }
}
