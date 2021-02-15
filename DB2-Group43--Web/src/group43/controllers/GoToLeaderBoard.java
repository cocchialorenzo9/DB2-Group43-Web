package group43.controllers;

import java.io.IOException;
import java.util.List;

import javax.ejb.EJB;
import javax.persistence.NonUniqueResultException;
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

import group43.entities.QuestionnaireInteraction;
import group43.exceptions.CredentialsException;
import group43.exceptions.QuestionnaireInteractionException;
import group43.services.QuestionnaireInteractionService;

/**
 * Servlet implementation class GoToLeaderBoard
 */
@WebServlet("/User/GoToLeaderBoard")
public class GoToLeaderBoard extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/QuestionnaireInteractionService")
    private QuestionnaireInteractionService qIS;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GoToLeaderBoard() {
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
		List<QuestionnaireInteraction> qI;
		
		try {
			qI = qIS.findInteractionOfTheDay();
		} catch (QuestionnaireInteractionException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Could not verify questionnaire interaction");
			return;
		}
		
		
		String path = "/WEB-INF/LeaderBoard.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("questionnaireInteractions", qI);
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
