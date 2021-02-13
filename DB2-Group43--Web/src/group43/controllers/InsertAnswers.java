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

@WebServlet("/User/InsertAnswers")
public class InsertAnswers extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/QuestionService")
	private QuestionService questService;
	@EJB(name = "group43.services/AnswerService")
	private AnswerService aService;
	@EJB(name = "group43.services/QuestionnaireInteractionService")
	private QuestionnaireInteractionService iService;
	@EJB(name = "group43.services/OffensiveWordService")
	private OffensiveWordService wordsService;
	@EJB(name = "group43.services/UserService")
	private UserService userService;
	
       
    public InsertAnswers() {
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
		Integer firstQuestionId = null;
		try {
			firstQuestionId = Integer.parseInt(request.getParameter("firstIndex"));
			System.out.println(firstQuestionId);
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
			toBeBlocked = processingMarketingSection(request, response, firstQuestionId, questionsNumber, word_list, answers);
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
			sex = StringEscapeUtils.escapeJava(request.getParameter("sex"));
			age = Integer.parseInt(request.getParameter("age"));
			explevel = StringEscapeUtils.escapeJava(request.getParameter("explevel"));
		} catch(NullPointerException e) {
			 e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error in processing the Statistical Session");
			return;
		}
		
		
		
		for(Integer i = 0; i < answers.size(); i++) {	
			System.out.println("INSERTING from servlet: " + answers.get(i) + " con id = " + firstQuestionId);
			try {
				aService.insertAnswer(user.getIduser(), answers.get(i), firstQuestionId);
			} catch (Exception e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong: impossible to insert the Marketing section answers");
				return;
			}
			
			firstQuestionId++;
		}
		
		try {
			iService.updateStatisticalSection(user.getIduser(), questionnaireId, age, sex, explevel);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "something went wrong: impossible to update the last interaction of the user");
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
	public boolean processingMarketingSection(HttpServletRequest request, HttpServletResponse response, int firstQuestionId, int questionsNumber, List<OffensiveWord> word_list, List<String> answers) throws IOException {
		String answerBody = null;
		boolean toBeBlocked = false;
		boolean invalidAnswers = false;
		
		for( Integer i = firstQuestionId; i < firstQuestionId + questionsNumber && !toBeBlocked; i++) {				
			try {	
				answerBody = StringEscapeUtils.escapeJava(request.getParameter(i.toString()));
					
				// required validity checks
				if(!invalidAnswers)
					invalidAnswers = checkForEmptyMarketingAnswers(answerBody);
				
				if(!toBeBlocked)
					toBeBlocked = checkForOffensiveWords(answerBody, word_list);
				
				System.out.println("ANSWER " + i + " is : " + answerBody);
				answers.add(answerBody);
					
			} catch (NumberFormatException | NullPointerException e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values -> answers marketing");
				return false;
			}
		}
		
		return toBeBlocked;
	}
	
	// This method checks for the user of an offensive word in a single text string (an answer body)
	public boolean checkForOffensiveWords(String text, List<OffensiveWord> word_list) {
		
		for(int i=0 ; i < word_list.size(); i++) {
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