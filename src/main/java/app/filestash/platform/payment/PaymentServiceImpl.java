package app.filestash.platform.payment;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import javax.annotation.PostConstruct;

import app.filestash.platform.payment.domain.CreditCard;
import app.filestash.platform.payment.domain.Invoice;
import com.stripe.model.*;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerListPaymentMethodsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.param.CustomerSearchParams;
import com.stripe.exception.StripeException;
import com.stripe.Stripe;

@Service
public class PaymentServiceImpl implements PaymentService {

	@Value("${stripe.token}")
	private String STRIPE_API_TOKEN;

	@Autowired
	CurrencyUtils currencyUtils;

	private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

	@PostConstruct
	public void StripeServiceSetup() {
		Stripe.apiKey = STRIPE_API_TOKEN;
	}

	@Override
	public List<CreditCard> getCards(String email) {
		ArrayList<String> cids = this.getCustomerId(email);
		ArrayList<CreditCard> cards = new ArrayList<CreditCard>();
		for(String cid : cids) {
            Customer customer = null;
            try {
                customer = Customer.retrieve(cid);
            } catch (StripeException e) {
                continue;
            }
            CustomerListPaymentMethodsParams params = CustomerListPaymentMethodsParams.builder()
					.setType(CustomerListPaymentMethodsParams.Type.CARD)
					.build();
            PaymentMethodCollection paymentMethods = null;
            try {
                paymentMethods = customer.listPaymentMethods(params);
            } catch (StripeException e) {
				logger.warn("STRIPE EXCEPTION msg={}", e.getMessage());
				continue;
            }
            List<PaymentMethod> pms = paymentMethods.getData();
			String hostedURL = this.getSession(cid).map(Session::getUrl).orElse("#");
			for (PaymentMethod pm: pms) {
				PaymentMethod.Card card = pm.getCard();
				cards.add(CreditCard.builder()
						.last4(card.getLast4())
						.type(card.getBrand())
						.expired(new GregorianCalendar(card.getExpYear().intValue(), card.getExpMonth().intValue(), 1).getTime())
						.url(hostedURL)
						.build());
			}
		}
		return cards;
	}

	@Override
	public List<Invoice> getInvoices(String email) {
		ArrayList<String> cids = this.getCustomerId(email);
		ArrayList<Invoice> invoices = new ArrayList<Invoice>();
		for (int i = 0; i<cids.size(); i++) {
			Map<String, Object> params = new HashMap<>();
			params.put("limit", 12);
			params.put("customer", cids.get(i));
            InvoiceCollection invcoll = null;
            try {
                invcoll = com.stripe.model.Invoice.list(params);
            } catch (StripeException e) {
				logger.warn("STRIPE EXCEPTION msg={}", e.getMessage());
                continue;
            }
			for(int j=0; j<invcoll.getData().size(); j++) {
				com.stripe.model.Invoice inv = invcoll.getData().get(j);
				invoices.add(Invoice.builder()
						.url(inv.getHostedInvoiceUrl())
						.amount(currencyUtils.format(inv.getCurrency(), inv.getAmountPaid()))
						.creationDate(LocalDate.from(Instant.ofEpochSecond(inv.getCreated())))
						.build());
			}
		}
		return invoices;
	}

	private ArrayList<String> getCustomerId(String email) {
		ArrayList<String> ids = new ArrayList<String>();
		CustomerSearchParams params = CustomerSearchParams
				.builder()
				.setQuery(String.format("email:'%s'", email))
				.build();
		try {
			CustomerSearchResult result = Customer.search(params);
			for (int i = 0; i < result.getData().size(); i++) {
				ids.add(result.getData().get(i).getId());
			}
		} catch (StripeException err) {
			return ids;
		}
		return ids;
	}

	private Optional<Session> getSession(String customerID) {
		Map<String, Object> params = new HashMap<>();
		params.put("customer", customerID);
        try {
            return Optional.of(Session.create(params));
        } catch (StripeException e) {
            return Optional.empty();
        }
    }
}
