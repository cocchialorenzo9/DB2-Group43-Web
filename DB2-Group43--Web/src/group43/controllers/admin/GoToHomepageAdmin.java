package group43.controllers.admin;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import group43.entities.User;
import group43.services.ProductService;

/**
 * Servlet implementation class GoToHomepageAdmin
 */
@WebServlet("/Admin/GoToHomepage")
public class GoToHomepageAdmin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	@EJB(name = "group43.services/ProductService")
	private ProductService prodService;

    public GoToHomepageAdmin() {
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
		
		// thanks to the filter, I am sure that exists a valid session and that user is logged as admin		
		// retrieve the idcreator from the session
		User admin = (User) request.getSession().getAttribute("user");
		int idcreator = admin.getIduser();
		
		// retrieve all the admin products
		List<Product> adminProducts = prodService.findProductsByCreatorId(idcreator);
		
		// retrieve all the product days
		// mapping each product with its integer id and its date in which it is product of the day
		Map<Integer, Date> productDays = new HashMap<>();
		for(Product p: adminProducts) {
			System.out.println(p.getIdproduct());
			System.out.println(p.getQuestionnaire().getIdquestionnaire());
			Date date = p.getQuestionnaire().getDate();
			productDays.put(p.getIdproduct(), date);
		}
		
		// set the "adminProducts", "productDays" on the Thymeleaf context
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		ctx.setVariable("adminProducts", adminProducts);
		ctx.setVariable("productDays", productDays);
		
		// redirect to AdminHomePage
		String path = "/WEB-INF/AdminHomePage.html";
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
