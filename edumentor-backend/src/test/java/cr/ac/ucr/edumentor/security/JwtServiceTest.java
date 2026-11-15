package cr.ac.ucr.edumentor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.ucr.edumentor.entity.Rol;
import cr.ac.ucr.edumentor.entity.Usuario;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "cqKhTKsEnlibNhociZiXLmWEv7kMckKUTWdwyzOltrK7Xu1FeJu6RTPwDie9IJz6";
    private static final String OTHER_SECRET = "dGhpcy1pcy1hbm90aGVyLXNlY3JldC1rZXktd2l0aC1lbm91Z2gtYnl0ZXM=";

    private Usuario tutorUser() {
        Usuario user = new Usuario("T1", "tutor@test.com", "hash", "Tomas", "Tutor", Rol.TUTOR);
        ReflectionTestUtils.setField(user, "id", 5L);
        return user;
    }

    @Test
    void generatesValidTokenWithExpectedSubject() {
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(1));

        String subject = jwt.validateAndGetSubject(jwt.generateToken(tutorUser())).orElseThrow();

        assertEquals("tutor@test.com", subject);
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(1));
        String token = jwt.generateToken(tutorUser());
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertTrue(jwt.validateAndGetSubject(tampered).isEmpty());
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = new JwtService(SECRET, Duration.ofSeconds(-5));

        assertTrue(jwt.validateAndGetSubject(jwt.generateToken(tutorUser())).isEmpty());
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        String token = new JwtService(OTHER_SECRET, Duration.ofHours(1)).generateToken(tutorUser());

        assertTrue(new JwtService(SECRET, Duration.ofHours(1)).validateAndGetSubject(token).isEmpty());
    }

    @Test
    void rejectsMalformedTokenWithoutThrowing() {
        assertTrue(new JwtService(SECRET, Duration.ofHours(1)).validateAndGetSubject("not.a.jwt").isEmpty());
    }

    @Test
    void requiresAtLeast256BitSecret() {
        assertThrows(WeakKeyException.class, () -> new JwtService("c2hvcnQ=", Duration.ofHours(1)));
    }
}
