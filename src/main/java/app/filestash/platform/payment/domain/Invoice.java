package app.filestash.platform.payment.domain;

import java.time.LocalDate;
import java.util.Date;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Invoice {
	public String url;
	public String amount;
	public LocalDate creationDate;
}
