package group43.filters;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import group43.entities.User;
import group43.utils.Roles;

@WebFilter(
		filterName = "Admin",
		urlPatterns = "/Admin/*"
		)
public class AdminFilter implements Filter {

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String loginpath = req.getServletContext().getContextPath() + "/index.html";
		
		System.out.println("Admin filtering ...");

		HttpSession s = req.getSession();
		User admin = (User) s.getAttribute("user");
		if (admin.getRole() != Roles.ADMIN) {
			System.out.println("Unauthorized");
			// rather than redirect, it can be possible even to print a Unauthorized page
			res.sendRedirect(loginpath);
		} else {
			System.out.println("Request by admin is ok");
			chain.doFilter(request, response);
		}
	}

}
