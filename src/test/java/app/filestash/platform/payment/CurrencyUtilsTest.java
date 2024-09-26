package app.filestash.platform.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CurrencyUtilsTest {
    @Autowired
    private CurrencyUtils currencyUtils;

    @Test
    void testFormat() {
        assertThat(currencyUtils.format("eur", 20000L)).isEqualTo("200€");
        assertThat(currencyUtils.format("aud", 20000L)).isEqualTo("$200");
        assertThat(currencyUtils.format("usd", 20000L)).isEqualTo("$200");
        assertThat(currencyUtils.format("xpf", 20000L)).isEqualTo("xpf200");
    }
}