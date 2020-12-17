package group43.controllers;

import java.io.IOException;

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

import group43.entities.Product;
import group43.entities.Questionnaire;
import group43.services.ProductService;
import group43.services.QuestionnaireService;

@WebServlet("/GetQuestionnaire")
public class GetQuestionnaire extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	@EJB(name = "group43.services/ProductService")
	private ProductService pService;
	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService qService;
       

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
		
		// Getting the questionnaire of the Day
		Questionnaire questionnaireOfTheDay = null;
		try {
			questionnaireOfTheDay = qService.findQuestionnaireById(1);
			System.out.print(questionnaireOfTheDay);
			if (questionnaireOfTheDay == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "No questionnaire of the day found");
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		// binding the questionnaire of the day with the product in the questionnaire
		Product product = null;
		try {
			product = pService.findProductById(1);
			if (product == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found");
				return;
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
		templateEngine.process(path, ctx, response.getWriter());
	}

	public void destroy() {
	}

}
