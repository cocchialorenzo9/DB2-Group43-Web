package group43.controllers.admin;

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
import org.apache.commons.lang.StringEscapeUtils;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.entities.*;
import group43.exceptions.AnswersException;
import group43.exceptions.QuestionnaireInteractionException;
import group43.services.*;

/**
 * Servlet implementation class GoToHomepageAdmin
 */
@WebServlet("/Admin/GetInspectionPage")
public class GetInspectionPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/ProductService")
	private ProductService prodService;
	@EJB(name = "group43.services/QuestionnaireInteractionService")
	private QuestionnaireInteractionService interactionsService;
	@EJB(name = "group43.services/AnswerService")
	private AnswerService ansService;


    public GetInspectionPage() {
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
				
		// retrieve the idproduct parameter to get the inspection page
		Integer idproduct = null;
		try {
			String idproductStr = StringEscapeUtils.escapeJava(request.getParameter("idproduct"));
			if(idproductStr.isEmpty() || idproductStr == null) {
				throw new NumberFormatException();
			}
			idproduct = Integer.parseInt(idproductStr);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id product not parsable");
			return;
		}
		
		// check coherency between id product and id creator
		Product prodToInspect = prodService.findProductById(idproduct);
		if(prodToInspect == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Id product not existent in the DB");
			return;
		}
		Questionnaire questionnaireToInspect = prodToInspect.getQuestionnaire();
		int idadmin = ((User) request.getSession().getAttribute("user")).getIduser();
		
		if(questionnaireToInspect.getUser().getIduser() != idadmin) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only the creator can inspect this questionnaire");
			return;
		}
		
		// get the information from services in order to complete
		// "completedUsernames", "cancelledUsernames" ,"allAnswers"
		List<String> completedUsernames = new ArrayList<>();
		List<String> cancelledUsernames = new ArrayList<>();
		List<Answer> allAnswers = new ArrayList<>();
		
		List<QuestionnaireInteraction> allQIs;
		try {
			allQIs = interactionsService.findInteractionsByQuestionnaireId(prodToInspect.getQuestionnaire().getIdquestionnaire());
		} catch (QuestionnaireInteractionException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		for(QuestionnaireInteraction interaction : allQIs) {
			String username = interaction.getUser().getUsername();
			if(interaction.isCompleted()) {
				// System.out.println("Adding " + username + " to completed list");
				completedUsernames.add(username);
			} else {
				// System.out.println("Adding " + username + " to cancelled list");
				cancelledUsernames.add(username);
			}
		}
		
		try {
			allAnswers = ansService.findAnswersByQuestionnaireId(questionnaireToInspect.getIdquestionnaire());
		} catch (AnswersException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}
		
		System.out.println("Num answers: " + allAnswers.size());
		
		// set the contect in thymeleaf to provide to the template
		// "product", "completedUsernames", "cancelledUsernames" ,"allAnswers"
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		ctx.setVariable("product", prodToInspect);
		ctx.setVariable("completedUsernames", completedUsernames);
		ctx.setVariable("cancelledUsernames", cancelledUsernames);
		ctx.setVariable("allAnswers", allAnswers);
		
		// redirect to the InspectionPage
		String path = "/WEB-INF/InspectionPage.html";
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}