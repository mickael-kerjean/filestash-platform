package app.filestash.platform.pages;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class IndexController {
	@GetMapping(path = "/")
	public String landingPage(Model model) {
		String thisMonth = "Fixed in " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
		try {
			model.addAttribute("hasAdvisory", Jsoup.connect("https://www.filestash.app/advisory.xml")
					.timeout(2000).parser(Parser.xmlParser()).get()
					.select("item > description").stream()
					.anyMatch(d -> d.text().startsWith(thisMonth)));
		} catch (Exception e) {}
		return "main";
	}
}
