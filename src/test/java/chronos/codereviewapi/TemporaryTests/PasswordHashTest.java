package chronos.codereviewapi.TemporaryTests;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHashTest {


        private final PasswordEncoder passwordEncoder =
                new BCryptPasswordEncoder();

        @Test
        void shouldEncodeAndVerifyPassword() {
            String rawPassword = "MyPassword123";

            String encodedPassword = passwordEncoder.encode(rawPassword);

            assertNotEquals(rawPassword, encodedPassword);
            assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
            assertFalse(passwordEncoder.matches("IncorrectPassword", encodedPassword));
        }
    }
