
package group43.controllers;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.entities.User;
import group43.exceptions.CredentialsException;
import group43.services.UserService;

/**
 * Servlet implementation class createUser
 */
@WebServlet("/CreateUser")
public class CreateUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	@EJB(name = "group43.services/UserService")
	private UserService usrService;

	public CreateUser() {
		super();
	}

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// obtain and escape params
		String usrn = null;
		String pwd1 = null;
		String pwd2 = null;
		String email = null;
		 usrn = StringEscapeUtils.escapeJava(request.getParameter("usrn"));
		 pwd1 = StringEscapeUtils.escapeJava(request.getParameter("pwd1"));
		 pwd2 = StringEscapeUtils.escapeJava(request.getParameter("pwd2"));
		 email = StringEscapeUtils.escapeJava(request.getParameter("email"));
		if (usrn == null || pwd1 == null || pwd2 == null || email ==null|| usrn.isEmpty() || pwd1.isEmpty() || pwd2.isEmpty()|| email.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("The credentials cannot be null");
			return;
		}
		
		Boolean validEmailAddress = isValidEmailAddress(email);
		
		Boolean equalPasswords = pwd1.equals(pwd2);
	
		Boolean usernameExists = null;
		try {
			usernameExists = usrService.usernameExists(usrn);
		} catch (CredentialsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Boolean emailExists = true;
		try {
			emailExists = usrService.emailExists(email);
		} catch (CredentialsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		

		String path;
		if (emailExists == true || usernameExists == true || equalPasswords == false || validEmailAddress == false) {
			ServletContext servletContext = getServletContext();
			path = "/registerPage.html";
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			if (emailExists == true) { 
				ctx.setVariable("errorMsg", "Email already used, please retry");
			} 
			if (usernameExists == true) { 
				ctx.setVariable("errorMsg", "Username already used, please retry");
			} 
			if (equalPasswords == false) { 
				ctx.setVariable("errorMsg", "The passwords are not equal");
			} 
			if (validEmailAddress == false) { 
				ctx.setVariable("errorMsg", "Email is not valid, please retry");
			} 
			templateEngine.process(path, ctx, response.getWriter());
		} else {
			
			usrService.createUser(usrn, email, pwd1);
			String ctxpath = getServletContext().getContextPath();
			path = ctxpath + "/index.html";
			response.sendRedirect(path);
		}

	}
	
	private boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
 }

	public void destroy() {
	}
}
