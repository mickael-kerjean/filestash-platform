package app.filestash.platform.pages;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {
	@GetMapping(path = "/")
	public String landingPage() {
		return "main";
	}
}
