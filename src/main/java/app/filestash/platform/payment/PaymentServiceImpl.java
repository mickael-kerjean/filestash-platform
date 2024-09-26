package app.filestash.platform.payment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

	private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

	@Value("${stripe.token}")
	private String STRIPE_API_TOKEN;

	@Autowired
	CurrencyUtils currencyUtils;

	@PostConstruct
	public void StripeServiceSetup() {
		if (!STRIPE_API_TOKEN.startsWith("sk_")) {
			logger.warn("STRIPE_TOKEN is not valid");
		}
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
            } catch (StripeException err) {
				logger.warn("paymentService::getCards action=customer.retrieve err={}", err.getMessage());
				continue;
            }
            CustomerListPaymentMethodsParams params = CustomerListPaymentMethodsParams.builder()
					.setType(CustomerListPaymentMethodsParams.Type.CARD)
					.build();
            PaymentMethodCollection paymentMethods = null;
            try {
                paymentMethods = customer.listPaymentMethods(params);
            } catch (StripeException err) {
				logger.warn("paymentService::getCards action=customer.listPaymentMethods err={}", err.getMessage());
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
            } catch (StripeException err) {
				logger.warn("paymentService::getInvoices action=invoice.list err={}", err.getMessage());
                continue;
            }
			for(int j=0; j<invcoll.getData().size(); j++) {
				com.stripe.model.Invoice inv = invcoll.getData().get(j);
				invoices.add(Invoice.builder()
						.url(inv.getHostedInvoiceUrl())
						.amount(currencyUtils.format(inv.getCurrency(), inv.getAmountPaid()))
						.creationDate(LocalDate.ofInstant(Instant.ofEpochSecond(inv.getCreated()), ZoneId.systemDefault())) // Convert Instant to LocalDate
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
			logger.warn("paymentService::getCustomerId action=customer.search err={}", err.getMessage());
			return ids;
		}
		return ids;
	}

	private Optional<Session> getSession(String customerID) {
		Map<String, Object> params = new HashMap<>();
		params.put("customer", customerID);
        try {
            return Optional.of(Session.create(params));
        } catch (StripeException err) {
			logger.warn("paymentService::getSession action=session.create err={}", err.getMessage());
            return Optional.empty();
        }
    }
}
