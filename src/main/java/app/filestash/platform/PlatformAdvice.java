package app.filestash.platform;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.ui.Model;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class PlatformAdvice {

	@ExceptionHandler(IllegalStateException.class)
	public String handleIllegalStateException(Model model, IllegalStateException ex, HttpServletResponse response) {
		model.addAttribute("status", HttpServletResponse.SC_BAD_REQUEST);
		model.addAttribute("message", ex.getMessage());
		model.addAttribute("exception", IllegalStateException.class.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return "error";
	}

	@ExceptionHandler(AccessDeniedException.class)
	public String handleAccessDeniedException(Model model, AccessDeniedException ex, HttpServletResponse response) {
		model.addAttribute("status", HttpServletResponse.SC_FORBIDDEN);
		model.addAttribute("message", ex.getMessage());
		model.addAttribute("exception", AccessDeniedException.class.toString());
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return "error";
	}
}
