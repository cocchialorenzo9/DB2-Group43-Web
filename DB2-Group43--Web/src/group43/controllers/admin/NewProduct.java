package group43.controllers.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import group43.entities.Product;
import group43.entities.Questionnaire;
import group43.entities.User;
import group43.services.ProductService;
import group43.services.QuestionService;
import group43.services.QuestionnaireService;

/*
 * Not only inserts a new product to the DB, but adds even a 
 * new questionnaire related to that product
 */

@WebServlet("/Admin/NewProduct")
public class NewProduct extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private TemplateEngine templateEngine;
	
	@EJB(name = "group43.services/ProductService")
	private ProductService prodService;
	@EJB(name = "group43.services/QuestionnaireService")
	private QuestionnaireService questService;
	@EJB(name = "group43.services/QuestionService")
	private QuestionService questionService;

    public NewProduct() {
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
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// check parameters {name, day, urlimg, List<question>}
		String errString = "ERROR ";
		boolean error = false;
		// retrieve data
		String name = request.getParameter("productName");
		String stringDate = request.getParameter("productDay");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			date = dateFormatter.parse(stringDate);
			System.out.println("date received: " + stringDate);
		} catch (ParseException e) {
			errString += ": date not parsable ";
			error = true;
		}
		String urlImage = request.getParameter("urlImg");
		int numQuestions = -1;
		try {
			numQuestions = Integer.parseInt(request.getParameter("numQuest"));
		} catch (NumberFormatException e) {
			errString += ": questions number not parsable ";
			error = true;
		}
		List<String> marketingQuestions = new ArrayList<>();
		if(numQuestions != -1) {
			for(int i = 0; i < numQuestions; i++) {
				String thisPar = "question" + i;
				String thisQuest = request.getParameter(thisPar);
				if(thisQuest == null) {
					i = numQuestions;
					errString += ": " + i + " question not retrievable";
					marketingQuestions = new ArrayList<>();
				} else {
					marketingQuestions.add(thisQuest);
				}
			}
		}
		
		if(name == null || stringDate == null || date == null ||
			urlImage == null || numQuestions == -1 || marketingQuestions.size() == 0) {
			// response.sendError(HttpServletResponse.SC_BAD_REQUEST, errString + ":: go back and correct the data");
			error = true;
		}
		
		if(date != null) {
			// checking date
			Calendar calToday = Calendar.getInstance();
			calToday.set(Calendar.HOUR_OF_DAY, 0);
			calToday.set(Calendar.MINUTE, 0);
			calToday.set(Calendar.SECOND, 0);
			Date today = calToday.getTime();
			if(date.before(today)) {
				errString += ": date inserted is before today, can not insert ";
				error = true;
			}
		}
		
		
		if(error) {
			errString += " >> try again.";
			// response.sendError(HttpServletResponse.SC_BAD_REQUEST, errString + ">> go back and correct the data");
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			
			ctx.setVariable("errorMessage", errString);
			
			String path = "/pages/KONewProduct.html";
			templateEngine.process(path, ctx, response.getWriter());
			return;
		}
				
		
		// call service to add the product
		User admin = ((User) request.getSession().getAttribute("user"));
		int idadmin = admin.getIduser();

		java.sql.Date sqlDate = new java.sql.Date(date.getTime());
		
		// Product newProduct = prodService.newProduct(name, urlImage);
		Product newProduct = new Product(name, urlImage);
		Questionnaire questionnaire = questService.newQuestionnaire(sqlDate, idadmin, newProduct);
		questionService.newQuestions(questionnaire, marketingQuestions);
		
		// redirect to ok message, or little AJAX call?
		// String path = request.getServletContext().getContextPath() + "/Admin/GoToHomepage";
		// response.sendRedirect(path);
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		String path = "/pages/OKNewProduct.html";
		templateEngine.process(path, ctx, response.getWriter());
	}

}
