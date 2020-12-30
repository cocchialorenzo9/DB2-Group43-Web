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
	@EJB(name = "group43.services/InteractionService")
	private InteractionService iService;
       

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
		
		// functional variable 
		String productErrorMsg = null;
		String questionErrorMsg = null;
		Integer questionsNumber = 0;
		Integer firstIndex = 0;
		Integer questionnaireId = 0;
		
		// Getting questionnaire of the Day
		try {
			questionnaireOfTheDay = qService.findQuestionnaireOfTheDay();
			if (questionnaireOfTheDay == null) {
				productErrorMsg = "No Questionnaire is available for today, try again tomorrow";
			}
			else { // If the is a questionnaire of the day
				questionnaireId = questionnaireOfTheDay.getIdquestionnaire();
				// getting the product of the day
				product = questionnaireOfTheDay.getProduct();
				
				// getting questionnaire's questions
				try {
					questions = questService.findQuestionsByQuestionnaireId(questionnaireOfTheDay.getIdquestionnaire());
					if (questions == null || questions.isEmpty())
						questionErrorMsg = "No questions available for the questionnaire of the day. Sorry";
					else 
					{
						questionsNumber = questions.size();
						firstIndex = questions.get(0).getIdquestion();
					}
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
					
				// Handling interaction and time stamp insertion or updating
				try {
					handleLogInteraction(user.getIduser(), questionnaireOfTheDay.getIdquestionnaire());
				}
				catch (LastInteractionException e) {
					e.printStackTrace();
					return;
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
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
	
	public boolean handleLogInteraction(int userId, int questionnaireId) throws LastInteractionException {
		
		// Checking if the user already has an interaction with the questionnaire
		QuestionnaireInteraction interaction = null;
		interaction = iService.findLastInteraction(userId, questionnaireId);
		
		// If no interaction is present, then add an interaction
		if(interaction == null)
			iService.insertInteraction(userId, questionnaireId);
		
		return true;
	}

	public void destroy() {}

}
