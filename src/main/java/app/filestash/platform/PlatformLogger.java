package app.filestash.platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class PlatformLogger implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(PlatformLogger.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute("requestStartTime", startTime);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        long startTime = (Long) request.getAttribute("requestStartTime");
        long endTime = System.currentTimeMillis();
        long executeTime = endTime - startTime;

        if (handler instanceof org.springframework.web.method.HandlerMethod) {
            StringBuilder sb = new StringBuilder();
            sb
                    .append("HTTP ")
                    .append(response.getStatus()).append(" ")
                    .append(request.getMethod()).append(" ")
                    .append(executeTime).append("ms ")
                    .append(request.getRequestURI()).append(" ");
            logger.info(sb.toString());
        }
    }
}
