package app.filestash.platform.payment;

import org.springframework.stereotype.Component;

@Component
public class CurrencyUtils {
    public String format(String currency, Long amount) {
        amount = amount / 100;
        return switch(currency.toUpperCase()) {
            case "USD" -> "$" + amount.toString();
            case "AUD" -> "$" + amount.toString();
            case "EUR" -> amount.toString() + "€";
            default -> currency + amount.toString();
        };
    }
}
