package app.filestash.platform.support;

import app.filestash.platform.payment.PaymentServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/support/")
public class SupportController {
	
	@Value("${filestash.form}")
	private String formTarget;

	@GetMapping("/ticket/new")
	public String createTicketPage(Model model, HttpServletRequest request) {
		if (request.getParameter("ok") != null) {
			model.addAttribute("message", "DONE");
			return "message";
		}
		model.addAttribute("target", formTarget);
		model.addAttribute("redirect", request.getRequestURL() + "?ok");
		return "support_ticket_creation";
	}
	
	@GetMapping("/book")
	public String createMeeting() {
		return "support_meeting";
	}
}
