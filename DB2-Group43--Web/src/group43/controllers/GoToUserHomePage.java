package group43.controllers;

import java.io.IOException;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.entities.Product;
import group43.entities.Questionnaire;
import group43.entities.Review;
import group43.exceptions.QuestionnaireException;
import group43.services.QuestionnaireService;

/**
 * Servlet implementation class GoToHomeUser
 */
@WebServlet("/User/GoToUserHomePage")
public class GoToUserHomePage extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService questionnaireService;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GoToUserHomePage() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// If the user is not logged in (not present in session) redirect to the login				
		Questionnaire questionnaireOfTheDay;
		try {
			questionnaireOfTheDay = questionnaireService.findQuestionnaireOfTheDay();
		} catch (QuestionnaireException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		String questionnaireNotCreated = "Questionnaire not yet created. Please try again later";
		
		Product productOfTheDay = null;
		
		List<Review> reviews = null;
		
		if(questionnaireOfTheDay != null) {
			productOfTheDay = questionnaireOfTheDay.getProduct();
			reviews = questionnaireOfTheDay.getProduct().getReviews();
		}
		
		String path = "/WEB-INF/UserHomePage.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("reviews", reviews);
		ctx.setVariable("productOfTheDay", productOfTheDay);
		ctx.setVariable("questionnaireNotCreated", questionnaireNotCreated);
		templateEngine.process(path, ctx, response.getWriter());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
