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

/*
 * There is no way to define an order between filter if not
 * making it explicit via web.xml file.
 * https://stackoverflow.com/questions/6560969/how-to-define-servlet-filter-order-of-execution-using-annotations-in-war
 * SessionFilter has to be set before of user and admin filters
 */

@WebFilter(
		filterName = "Session",
		urlPatterns = {"/Admin/*", "/User/*"}
)
public class SessionFilter implements Filter {

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String loginpath = req.getServletContext().getContextPath() + "/index.html";
		
		HttpSession s = req.getSession();
		if (s.isNew() || s.getAttribute("user") == null) {
			System.out.println("This request had no valid session, redirecting...");
			res.sendRedirect(loginpath);
		} else {
			chain.doFilter(request, response);
		}
	}

}
