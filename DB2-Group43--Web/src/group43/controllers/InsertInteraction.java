package group43.controllers;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.entities.Questionnaire;
import group43.entities.QuestionnaireInteraction;
import group43.entities.User;
import group43.exceptions.LastInteractionException;
import group43.services.QuestionnaireInteractionService;
import group43.services.QuestionnaireService;


@WebServlet("/User/InsertInteraction")
public class InsertInteraction extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	@EJB(name = "group43.services/QuestionnaireInteractionService")
	private QuestionnaireInteractionService iService;
	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService qService;

    public InsertInteraction() {
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
	

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Retrieve the current session
		HttpSession session = request.getSession();
		
		// Retrieve the user from the Session
		User user = (User) session.getAttribute("user");
		

		// Declaring the useful Entities
		Questionnaire questionnaireOfTheDay = null;
		
		// Getting questionnaire of the Day
		try {
			questionnaireOfTheDay = qService.findQuestionnaireOfTheDay();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}	
		
		// Checking if the user already has an interaction with the questionnaire
		QuestionnaireInteraction interaction = null;
		try {
			interaction = iService.findLastInteraction(user.getIduser(), questionnaireOfTheDay.getIdquestionnaire());
		} catch (LastInteractionException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in searching for the last interaction of the user");
		}
				
		// If no interaction is present, then add an interaction
		if(interaction == null)
			iService.insertInteractionWithoutStatistics(user.getIduser(), questionnaireOfTheDay.getIdquestionnaire());
		else
			try {
				iService.UpdateInteraction(interaction.getIdquestionnaire_interaction());
			} catch (LastInteractionException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in updating the last interaction of the user");
			}
		
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/User/GoToUserHomePage";
		response.sendRedirect(path);

	}
	
	public void destroy() {}


}
