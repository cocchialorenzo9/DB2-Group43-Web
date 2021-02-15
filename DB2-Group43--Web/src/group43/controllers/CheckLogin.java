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
import org.apache.commons.lang.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.services.UserService;
import group43.entities.User;
import group43.exceptions.CredentialsException;
import javax.persistence.NonUniqueResultException;

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	@EJB(name = "group43.services/UserService")
	private UserService usrService;

	public CheckLogin() {
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
		String pwd = null;
		try {
			usrn = StringEscapeUtils.escapeJava(request.getParameter("usrn"));
			pwd = StringEscapeUtils.escapeJava(request.getParameter("pwd"));
			if (usrn == null || pwd == null || usrn.isEmpty() || pwd.isEmpty()) {
				throw new Exception("Missing or empty credential value");
			}

		} catch (Exception e) {
			// for debugging only e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credential value");
			return;
		}
		User user;
		try {
			// query db to authenticate for user
			user = usrService.checkCredentials(usrn, pwd);
		} catch (CredentialsException | NonUniqueResultException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not check credentials");
			return;
		}

		// If the user exists, add info to the session and go to home page, otherwise
		// show login page with error message

		String path;
		if (user == null) {
			ServletContext servletContext = getServletContext();
			path = "/index.html";
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			if (usrn.isEmpty() || pwd.isEmpty()) { //usrn o pwd non compilati
				ctx.setVariable("errorMsg", "Username or password are empty, please retry");
			} 
			if(!usrn.isEmpty() || !pwd.isEmpty()){
				//usrn o pwd non corretti
				ctx.setVariable("errorMsg", "Username or password are not correct");
			}
			
			templateEngine.process(path, ctx, response.getWriter());
			
		} 
		else if(user.isBlocked() == true) {
			ServletContext servletContext = getServletContext();
			path = "/index.html";
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsg", "You are permanently blocked. Access denied.");
			templateEngine.process(path, ctx, response.getWriter());
		}
		else {
			
			
			/*
			request.getSession().setAttribute("Utente", u);
			String target = (u.getRuolo().equals("Impiegato")) ? "/GoToHomeImpiegato" : "/GoToHomeCliente";
			path = getServletContext().getContextPath() + target;
			response.sendRedirect(path);
			request.getSession().setAttribute("user", user);
			path = getServletContext().getContextPath() + "/Home";
			response.sendRedirect(path);
			request.getSession().setAttribute("Utente", u);
			String target = (u.getRuolo().equals("Impiegato")) ? "/GoToHomeImpiegato" : "/GoToHomeCliente";
			path = getServletContext().getContextPath() + target;
			response.sendRedirect(path);
			*/
			request.getSession().setAttribute("user", user);
			String target = (user.getRole().toString().equals("user")) ? "/User/GoToUserHomePage" : "/Admin/GoToHomepage";
			path = getServletContext().getContextPath() + target;
			response.sendRedirect(path);
		}

	}

	public void destroy() {
	}
}