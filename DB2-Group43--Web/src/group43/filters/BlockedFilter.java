package group43.filters;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import group43.entities.User;
import group43.utils.Roles;

/**
 * Servlet Filter implementation class BlockedFIlter
 */
@WebFilter("/BlockedFilter")
public class BlockedFilter implements Filter {

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String blockedPath = req.getServletContext().getContextPath() + "/BlockedPage.html";
		
		System.out.println("Blocked filtering ...");

		HttpSession s = req.getSession();
		User user = (User) s.getAttribute("user");
		
		if (user.isBlocked()) {
			System.out.println("Blocked");
			
			s.invalidate();
			
			res.sendRedirect(blockedPath);
		} else {
			System.out.println("Request by user is ok");
			chain.doFilter(request, response);
		}
	}


}
