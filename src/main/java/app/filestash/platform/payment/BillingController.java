package app.filestash.platform.payment;

import app.filestash.platform.session.SessionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.swing.text.html.Option;
import java.nio.file.AccessDeniedException;
import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/support/")
public class BillingController {

    @Autowired
    PaymentService paymentService;

    @GetMapping("/billing")
    public String billingPage(Model model, Principal principal) throws AccessDeniedException {
        return Optional.ofNullable(principal)
                .map(Principal::getName)
                .map(email -> {
                    model.addAttribute("invoices", paymentService.getInvoices(email));
                    return "billing_invoices";
                })
                .orElseThrow(() -> new AccessDeniedException("Not Authenticated"));
    }

    @GetMapping("/cards")
    public String cardPage(Model model, Principal principal) throws AccessDeniedException {
        return Optional.ofNullable(principal)
                .map(Principal::getName)
                .map(email -> {
                    model.addAttribute("cards", paymentService.getCards(email));
                    return "billing_cards";
                })
                .orElseThrow(() -> new AccessDeniedException("Not Authenticated"));
    }
}
