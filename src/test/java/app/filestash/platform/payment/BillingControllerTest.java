package app.filestash.platform.payment;

import app.filestash.platform.payment.domain.CreditCard;
import app.filestash.platform.payment.domain.Invoice;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void testBillingPage() throws Exception {
        // given
        Principal mockPrincipal = Mockito.mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("user@example.com");
        when(paymentService.getInvoices("user@example.com")).thenReturn(List.of(
                Invoice.builder().creationDate(LocalDate.now()).amount("$400").build()
        ));

        // when
        Document document = Jsoup.parse(mockMvc.perform(get("/support/billing").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // then
        verify(paymentService).getInvoices("user@example.com");
        assertThat(document.select("h2").text()).contains("Billing");
        assertThat(document.select(".card").size()).isEqualTo(1);
    }

    @Test
    void testCardsPage() throws Exception {
        // given
        Principal mockPrincipal = Mockito.mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("user@example.com");
        when(paymentService.getCards("user@example.com")).thenReturn(List.of(
                CreditCard.builder().type("visa").last4("0420").build()
        ));

        // when
        Document document = Jsoup.parse(mockMvc.perform(get("/support/cards").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // then
        verify(paymentService).getCards("user@example.com");
        assertThat(document.select("h2").text()).contains("Payment Cards");
        assertThat(document.select(".card").size()).isEqualTo(1);
    }

    @Test
    void testNotAuthenticated() throws Exception {
        mockMvc.perform(get("/support/billing"))
                .andExpect(status().is(403));

        mockMvc.perform(get("/support/cards"))
                .andExpect(status().is(403));
    }
}