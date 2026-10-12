package by.bsuir.insurance.server.security;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Модульные тесты хеширования паролей. */
public class PasswordUtilTest {

    @Test
    public void hashIsStableAndSalted() {
        String salt = PasswordUtil.newSalt();
        assertEquals(32, salt.length());
        String h = PasswordUtil.hash("client123", salt);
        assertEquals(64, h.length());
        assertEquals(h, PasswordUtil.hash("client123", salt));
        assertNotEquals(h, PasswordUtil.hash("client123", PasswordUtil.newSalt()));
    }

    @Test
    public void matchesChecksPassword() {
        String salt = PasswordUtil.newSalt();
        String h = PasswordUtil.hash("secret1", salt);
        assertTrue(PasswordUtil.matches("secret1", salt, h));
        assertFalse(PasswordUtil.matches("secret2", salt, h));
    }
}
