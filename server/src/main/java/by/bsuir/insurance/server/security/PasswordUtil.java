package by.bsuir.insurance.server.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Хеширование паролей: SHA-256 от соли и пароля. */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** Новая случайная соль: 16 байт в шестнадцатеричном виде (32 символа). */
    public static String newSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return toHex(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }

    /** Сравнение за постоянное время, чтобы не раскрывать совпадение по времени ответа. */
    public static boolean matches(String password, String salt, String expectedHash) {
        return MessageDigest.isEqual(hash(password, salt).getBytes(StandardCharsets.US_ASCII),
                expectedHash.trim().getBytes(StandardCharsets.US_ASCII));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
