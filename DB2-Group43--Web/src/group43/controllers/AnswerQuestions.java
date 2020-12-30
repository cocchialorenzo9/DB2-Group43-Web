package group43.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.services.*;
import group43.entities.*;

@WebServlet("/User/AnswerQuestions")
public class AnswerQuestions extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/QuestionService")
	private QuestionService questService;
	@EJB(name = "group43.services/AnswerService")
	private AnswerService aService;
	@EJB(name = "group43.services/InteractionService")
	private InteractionService iService;
	@EJB(name = "group43.services/OffensiveWordService")
	private OffensiveWordService wordsService;
	@EJB(name = "group43.services/UserService")
	private UserService userService;
	
       
    public AnswerQuestions() {
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

		// Retrieving the current session
		HttpSession session = request.getSession();
		
		// Retrieve the user from the Session
		User user = (User) session.getAttribute("user");
		
		// Retrieve the number of questions answered from the request
		Integer questionsNumber = null;
		try {
			questionsNumber = Integer.parseInt(request.getParameter("questionsNumber"));
		} catch (NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> questionsNumber");
			return;
		}
		
		// Retrieve the first index of the questionsList to start the iteration correctly
		Integer firstIndex = null;
		try {
			firstIndex = Integer.parseInt(request.getParameter("firstIndex"));
		} catch (NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> first Index");
			return;
		}
		
		// Retrieve the questionnaireId
		Integer questionnaireId = null;
		try {
			questionnaireId = Integer.parseInt(request.getParameter("questionnaireId"));
		} catch (NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> questionnare id");
			return;
		}
		
		// Retrieving the list of offensive words to check for 
		List<OffensiveWord> word_list = null;
		try {
			word_list = wordsService.findAllOffensiveWords();
			if(word_list == null) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No Offensive word in the database");
				return;
			}
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in checking for offensive words");
			return;
		}
		
		boolean toBeBlocked = false;
		
		// Processing the answers from the Marketing Sessions, checking for the offensive words list and retrieving
		// a boolean to know whether the user has to be blocked or not
		List<String> answers = new ArrayList<String>();
		try {
			toBeBlocked = processingMarketingSection(request, response, firstIndex, questionsNumber, word_list, answers);
		} catch(IOException e) {
			 e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error in processing the Marketing Session");
			return;
		}
		
		// Checking if the number of answers is equals to the number of questions presents in the questionnaire
		if(!CorrectNumberOfAnswer(questionsNumber, questionnaireId)) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Wrong number of answers respect to the number of question in the questionnaire");
			return;
		}
		
		//If the user must be blocked because he used some of the offensive words in the list
		if(toBeBlocked) {
			// blocking profile
			try {
				user.setBlocked(true);
				userService.updateProfile(user);
			} 
			catch(Exception e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in answering the marketing section");
				user.setBlocked(false);
				return;
			}
			
			// Redirecting to BlockedPage.html
			System.out.println("Bloccato");
			String ctxpath = getServletContext().getContextPath();
			String path = ctxpath + "/User/GetBlockedPage";
			response.sendRedirect(path);
			return ;

		}
		
		String sex = null;
		Integer age = null;
		String explevel = null;
		try {
			processingStatisticalSection(request, response, sex, age, explevel);
		} catch(IOException e) {
			 e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error in processing the Statistical Session");
			return;
		}
			
		// Searching for the last interaction of the user with the questionnaire of the day
		QuestionnaireInteraction interaction = null;
		try {
			interaction = iService.findLastInteraction(user.getIduser(), questionnaireId);
			if(interaction == null) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong: impossible to retrieve the last interaction of the user");
				return;
			}	
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong: impossible to retrieve the last interaction of the user");
			return;
		}

		// If no problems occurs, then answers of the marketing section can be added to the database
		if(!insertingMarketingAnswers(firstIndex,  questionsNumber, user.getIduser(), answers)) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong: impossible to insert the Marketing section answers");
			return;
		}
		
		// Now update the interaction, setting the completed values and the answers of the statistical section
		try {
			
			// Since the answers are not mandatory, checking the nullness of the fized inputs answers
			if(age != null)
				interaction.setAge(age);
			if(explevel != null)
				interaction.setExpertise_level(explevel);
			if(sex != null)
				interaction.setSex(sex);
			
			interaction.setCompleted(true);
			iService.updateStatisticalSection(interaction);
		} 
		catch(Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong in inserting statistical section answers");
			interaction.setAge(0);
			interaction.setExpertise_level(null);
			interaction.setSex(null);
			return;
		}
			
		// Redirecting the User to the greetings Page
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/User/GetGreetingsPage";
		response.sendRedirect(path);
		return ;
	}

	public boolean CorrectNumberOfAnswer(int questionsNumber, int questionnaireId) {
		Integer count = 0;
		
		try {
			count = questService.findQuestionsByQuestionnaireId(questionnaireId).size();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(count == questionsNumber)
			return true;
		else 
			return false;
	}
	
	
	// This method processes the Marketing Section answers by retrieving the request parameters and checking
	// for the use of offensive Words
	public boolean processingMarketingSection(HttpServletRequest request, HttpServletResponse response, int firstIndex, int questionsNumber, List<OffensiveWord> word_list, List<String> answers) throws IOException {
		String answerBody = null;
		boolean toBeBlocked = false;
		boolean invalidAnswers = false;
		
		for( Integer i = firstIndex; i <= questionsNumber && !toBeBlocked; i++) {				
			try {	
				answerBody = StringEscapeUtils.escapeJava(request.getParameter(i.toString()));
					
				// required validity checks
				if(!invalidAnswers)
					invalidAnswers = checkForEmptyMarketingAnswers(answerBody);
				
				if(!toBeBlocked)
					toBeBlocked = checkForOffensiveWords(answerBody, word_list);
					
				answers.add(answerBody);
					
			} catch (NumberFormatException | NullPointerException e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> answers marketing");
				return false;
			}
		}
		
		return toBeBlocked;
	}
	
	// This method processes the Statistical Section answers by retrieving the request parameters 
	public void processingStatisticalSection(HttpServletRequest request, HttpServletResponse response, String sex, Integer age, String explevel) throws IOException {

		try {
			sex = StringEscapeUtils.escapeJava(request.getParameter("sex"));
		} catch (NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> sex");
			return ;
		}
		

		try {
			age = Integer.parseInt(request.getParameter("age"));
		} catch (NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> age");
			return ;
		}
		

		try {
			explevel = StringEscapeUtils.escapeJava(request.getParameter("explevel"));
		} catch (NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> exp level");
			return ;
		}
		
		return ;
	}
	
	// This method inserts in the db the Marketing questions answers
	public boolean insertingMarketingAnswers(int firstIndex, int questionsNumber, int userId, List<String> answers) {
		int j = 0;
		
		for( Integer i = firstIndex; i <= questionsNumber; i++) {	
			
			try {
				aService.insertAnswer(userId, answers.get(j), i);
			} catch (Exception e) {
				return false;
			}
			
			j++;
		}
		
		return true;
	}
	
	// This method checks for the user of an offensive word in a single text string (an answer body)
	public boolean checkForOffensiveWords(String text, List<OffensiveWord> word_list) {
		
		for(int i=0 ; i< word_list.size(); i++) {
			System.out.println(text + " " + word_list.get(i).getWord());
			if (text.contains(word_list.get(i).getWord()))
				return true;
		}
		
		return false;
	}
	
	// This method checks for the emptiness of a text string (an answer body)
	public boolean checkForEmptyMarketingAnswers(String text) {
		
		if(text == null || text.isBlank())
			return true;
		else 
			return false;
	}
	
	
	public void destroy() {}

}
