package app.filestash.platform.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class SessionController {
		
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

	@Autowired
	SessionCodeService codeService;
	
	@GetMapping("/login")
	public String loginForm(HttpServletRequest request) {
		return "login";
	}

	@PostMapping("/login/sendcode")
	public String sendCodePage(Model model, @RequestBody MultiValueMap<String, String> params, HttpServletRequest request) {
		String email = params.getFirst("username");
		String baseURL = request.getRequestURL().toString().replaceAll(request.getRequestURI(), "");
		logger.info("login link - {}/login/callback?email={}&code={}", baseURL, email, codeService.generateCode(email));
		model.addAttribute("message", "Done");
		model.addAttribute("description", "Ask support for your link");
		return "message";
	}
	
	@GetMapping("/login/callback")
	public String loginWithCodePage(HttpServletRequest request) {
		String email = request.getParameter("email");
		String code = request.getParameter("code");
		String next = request.getParameter("next");
		
		if (!codeService.verifyCode(email, code)) {
			throw new IllegalStateException("Wrong link");
		}
		this.setRole(request, email, "ROLE_CHAMPION");
		if (next != null) {
			return String.format("redirect:%s", next);
		}
		return "redirect:/";
	}

	@GetMapping("/login/anonymous")
	public String loginAnonymous(HttpServletRequest req, @RequestParam(value = "origin", defaultValue = "na") String origin) {
		switch (origin) {
		case "aws":
		case "zendesk":
		case "trello":
			this.setRole(req, origin, "ROLE_PARTNER");
			break;
		default:
			throw new IllegalStateException("Invalid Origin");
		}
		return "redirect:/";
	}

	@GetMapping("/logout")
	public String logoutPage(HttpSession session) {
		session.invalidate();
		return "redirect:/";
	}
	
	private void setRole(HttpServletRequest request, String origin, String role) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			origin, null, AuthorityUtils.createAuthorityList(role)
		);
		SecurityContext sc = SecurityContextHolder.getContext();
		sc.setAuthentication(authentication);
		HttpSession session = request.getSession(true);
	    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
	}
}
