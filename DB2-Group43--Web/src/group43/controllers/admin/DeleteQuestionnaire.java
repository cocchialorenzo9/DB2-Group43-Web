package group43.controllers.admin;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import org.apache.commons.lang.StringEscapeUtils;

import group43.entities.Questionnaire;
import group43.entities.User;
import group43.services.QuestionnaireService;

/**
 * Servlet implementation class GoToHomepageAdmin
 */
@WebServlet("/Admin/DeleteQuestionnaire")
public class DeleteQuestionnaire extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService questService;

    public DeleteQuestionnaire() {
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
		// retrieve questionnaire from request parameter idproduct
		Integer idproduct = null;
		try {
			String idproductStr = StringEscapeUtils.escapeJava(request.getParameter("idproduct"));
			if(idproductStr.isEmpty() || idproductStr == null) {
				throw new NumberFormatException();
			}
			idproduct = Integer.parseInt(request.getParameter("idproduct"));
		} catch (NumberFormatException e) {
			response.sendError(505, "Questionnaire id not parsable");
			return;
		}
		
		Questionnaire questToDelete = questService.findQuestionnaireByProductid(idproduct);
		
		// check coherency between idcreator and questionnaire
		int idadmin = ((User) request.getSession().getAttribute("user")).getIduser();
		if(questToDelete.getUser().getIduser() != idadmin) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only the cretor can delete the questionnaire");
			return;
		}
		
		// check if date is before of today
		Calendar calToday = Calendar.getInstance();
		calToday.set(Calendar.HOUR_OF_DAY, 0);
		calToday.set(Calendar.MINUTE, 0);
		calToday.set(Calendar.SECOND, 0);
		calToday.set(Calendar.MILLISECOND, 0);
		Date today = calToday.getTime();
		if(today.before(questToDelete.getDate())) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You can delete only dates before of today");
			return;
		}
		
		// call service to delete questionnaire
		int retCode = questService.deleteQuestionnaire(questToDelete.getIdquestionnaire());
		
		// redirect to ok, ko pages
		String path = "";
		if(retCode == -1) {
			path = "/WEB-INF/KODelete.html";
		} else {
			path = "/WEB-INF/OKDelete.html";
		}
		
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		templateEngine.process(path, ctx, response.getWriter());
	}


	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}