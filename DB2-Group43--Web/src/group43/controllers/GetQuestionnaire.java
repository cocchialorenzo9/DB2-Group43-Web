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

import group43.entities.*;
import group43.exceptions.LastInteractionException;
import group43.services.*;


@WebServlet("/User/GetQuestionnaire")
public class GetQuestionnaire extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	@EJB(name = "group43.services/ProductService")
	private ProductService pService;
	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService qService;
	@EJB(name = "group43.services/QuestionService")
	private QuestionService questService;
	@EJB(name = "group43.services/QuestionnaireInteractionService")
	private QuestionnaireInteractionService iService;
       

    public GetQuestionnaire() {
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
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		// Retrieve the current session
		HttpSession session = request.getSession();
		
		// Retrieve the user from the Session
		User user = (User) session.getAttribute("user");
		
		
		// Declaring the useful Entities
		Questionnaire questionnaireOfTheDay = null;
		Product product = null;
		List<Question> questions = null;
		QuestionnaireInteraction interaction = null;
		
		// functional variable 
		String productErrorMsg = null;
		String questionErrorMsg = null;
		Integer questionsNumber = 0;
		Integer firstIndex = 0;
		Integer questionnaireId = 0;
		
		// Getting questionnaire of the Day
		try {
			questionnaireOfTheDay = qService.findQuestionnaireOfTheDay();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}	
		
		if (questionnaireOfTheDay == null) 
			productErrorMsg = "No Questionnaire is available for today, try again tomorrow";
		else { 
			// If the is a questionnaire of the day
			questionnaireId = questionnaireOfTheDay.getIdquestionnaire();
				
			try {
				// if the questionnaire exists, retrieving the last interaction of the user
				// to check whether he already filled up a questionnaire or not
				interaction = iService.findLastInteraction(user.getIduser(), questionnaireId);
			} catch (Exception e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in searching for the last interaction of the user");
				return;
			}
			
			// if the user already filled up the questionnaire
			if(interaction != null && interaction.isCompleted()) {
				// Redirect to the already filled page and add error message to be displayed
				String path = "/WEB-INF/AlreadyFilledPage.html";
				ServletContext servletContext = getServletContext();
				final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
				templateEngine.process(path, ctx, response.getWriter());
				return ;
			}
			
			// getting the product of the day
			product = questionnaireOfTheDay.getProduct();
			// getting the questions of the questionnaire of the day
			questions = questionnaireOfTheDay.getQuestions();

			if (questions == null || questions.isEmpty())
				questionErrorMsg = "No questions available for the questionnaire of the day. Sorry";
			else {
				// getting useful parameters for answering settings
				questionsNumber = questions.size();
				firstIndex = questions.get(0).getIdquestion();
			}

		}
		
		// Redirect to the Home page and add product to the parameters
		String path = "/WEB-INF/QuestionnairePage.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("product", product);
		ctx.setVariable("questions", questions);
		ctx.setVariable("productErrorMsg", productErrorMsg);
		ctx.setVariable("questionErrorMsg", questionErrorMsg);
		ctx.setVariable("questionsNumber", questionsNumber);
		ctx.setVariable("firstIndex", firstIndex);
		ctx.setVariable("questionnaireId", questionnaireId);
		templateEngine.process(path, ctx, response.getWriter());
	}
	

	public void destroy() {}

}
