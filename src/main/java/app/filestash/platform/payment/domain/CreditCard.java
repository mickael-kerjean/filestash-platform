package app.filestash.platform.payment.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CreditCard {
	public String url;
	public String type;
	public String last4;
	public Date expired;
}
