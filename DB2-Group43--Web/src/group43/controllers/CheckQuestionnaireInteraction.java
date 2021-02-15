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
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.entities.Questionnaire;
import group43.entities.QuestionnaireInteraction;
import group43.entities.User;
import group43.services.QuestionnaireInteractionService;
import group43.services.QuestionnaireService;

/**
 * Servlet implementation class CheckQuestionnaireInteraction
 */
@WebServlet("/User/CheckQuestionnaireInteraction")
public class CheckQuestionnaireInteraction extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;

	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService qService;
	@EJB(name = "group43.services/QuestionnaireInteractionService")
	private QuestionnaireInteractionService iService;

	
    public CheckQuestionnaireInteraction() {
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

		HttpSession session = request.getSession();
		
		// Retrieve the user through the Session
		User user = (User) session.getAttribute("user");
		
		// Declaring the entities that the servlet is going to user
		Questionnaire questionnaireOfTheDay = null;
		Integer questionnaireId = 0;
		QuestionnaireInteraction interaction = null;
		
		try {
			// Retrieving the Questionnaire of the day, if present
			questionnaireOfTheDay = qService.findQuestionnaireOfTheDay();
			if (questionnaireOfTheDay != null) 
			{
				questionnaireId = questionnaireOfTheDay.getIdquestionnaire();
				try {
					// if the questionnaire exists, retrieving the last interaction of the user
					// to check whether he already filled up a questionnaire or not
					interaction = iService.findLastInteraction(user.getIduser(), questionnaireId);
				} catch (Exception e) {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in searching for the last interaction of the user");
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		// If he already interacted with the questionnaire redirect to AlreadyFilledPage.html
		if(interaction != null && interaction.isCompleted()) {
			// Redirect to the already filled page and add error message to be displayed
			String path = "/WEB-INF/AlreadyFilledPage.html";
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			templateEngine.process(path, ctx, response.getWriter());
		}
		else {
			// If he didn't filled up the questionnaire of the day already, or didn't completed it
			// redirecting to the Questionnaire Page trough the dedicated servlet
			String ctxpath = getServletContext().getContextPath();
			String path = ctxpath + "/User/GetQuestionnaire";
			response.sendRedirect(path);
		}
	}


}
