package app.filestash.platform.payment;

import java.util.List;

import app.filestash.platform.payment.domain.CreditCard;
import app.filestash.platform.payment.domain.Invoice;

public interface PaymentService {
	public List<CreditCard> getCards(String email);
	public List<Invoice> getInvoices(String email);
}
