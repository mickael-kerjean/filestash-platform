package app.filestash.platform.pages;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SkeletonController {

	@GetMapping(path = "/healthz")
	public String pageHealthCheck() {
		return "ok";
	}
	
	@GetMapping(path = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
	public String pageRobotTxt() {
		return """
User-agent: *
Disallow: /
""";
	}
}
