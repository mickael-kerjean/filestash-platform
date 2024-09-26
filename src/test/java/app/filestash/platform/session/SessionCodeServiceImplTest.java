package app.filestash.platform.session;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SessionCodeServiceImplTest {

    @Autowired
    SessionCodeService sessionCodeService;

    @Test
    void testCodeGeneration() {
        String code = sessionCodeService.generateCode("hello@world.com");
        Assertions.assertThat(code).isNotEmpty();
    }

    @Test
    void testCodeVerify() {
        String key = "test@example.com";
        String code = sessionCodeService.generateCode(key);
        Assertions.assertThat(sessionCodeService.verifyCode(key, code)).isEqualTo(true);
    }
}