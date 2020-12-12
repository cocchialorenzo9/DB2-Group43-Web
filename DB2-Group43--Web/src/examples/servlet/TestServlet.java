package examples.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;

import examples.model.User;
import examples.stateless.UserService;

@WebServlet(name="TestServlet", 
            urlPatterns="/TestServlet")
public class TestServlet extends HttpServlet {

    private final String TITLE = 
        "Chapter 3: Employee Service Example";
    
    private final String DESCRIPTION = 
        "This example has been made to test our project. Remeber to add manually " +
        "a user in your database, then run this application and click on FindAll " +
        "to find every user you added</br>";

    
    // Inject a reference to the UserService slsb
    @EJB UserService service;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        printHtmlHeader(out);
        
        // process request
        String action = request.getParameter("action");
        if (action == null) {
            // do nothing if no action requested
        } else if (action.equals("Create")) {
            User emp = service.createUser(
                    parseInt(request.getParameter("createId")),
                    request.getParameter("name"));
            out.println("Created " + emp);
        } else if (action.equals("Remove")) {
            String id = request.getParameter("removeId");
            User emp = service.removeUser(parseInt(id));
            out.println("Removed " + emp);
        } else if (action.equals("Find")) {
            User emp = service.findUser(
                    parseInt(request.getParameter("findId")));
            out.println("Found " + emp);
        } else if (action.equals("FindAll")) {
            Collection<User> emps = service.findAllUsers();
            if (emps.isEmpty()) {
                out.println("No User found, add them by MySQL :) ");
            } else {
                out.println("Found Users: </br>");
                for (User emp : emps) {
                    out.print(emp + "<br/>");
                }
            }
        }
        
        printHtmlFooter(out);
    }
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
    
    private int parseInt(String intString) {
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private long parseLong(String longString) {
        try {
            return Long.parseLong(longString);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void printHtmlHeader(PrintWriter out) throws IOException {
        out.println("<body>");
        out.println("<html>");
        out.println("<head><title>" + TITLE + "</title></head>");
        out.println("<center><h1>" + TITLE + "</h1></center>");
        out.println("<p>" + DESCRIPTION + "</p>");
        out.println("<hr/>");
        out.println("<form action=\"TestServlet\" method=\"POST\">");
        // form to create
        out.println("<h3>Create an User</h3>");
        out.println("<table><tbody>");
        out.println("<tr><td>Id:</td><td><input type=\"text\" name=\"createId\"/>(int)</td></tr>");
        out.println("<tr><td>Username:</td><td><input type=\"text\" name=\"name\"/>(String)</td></tr>");
        out.println("</tbody></table>");
        out.println("<hr/>");
        // form to remove
        out.println("<h3>Remove a User</h3>");
        out.println("<table><tbody>");
        out.println("<tr><td>Id:</td><td><input type=\"text\" name=\"removeId\"/>(int)</td>" +
                    "<td><input name=\"action\" type=\"submit\" value=\"Remove\"/></td></tr>");
        out.println("</tbody></table>");
        out.println("<hr/>");
        // form to find
        out.println("<h3>Find a User</h3>");
        out.println("<table><tbody>");
        out.println("<tr><td>Id:</td><td><input type=\"text\" name=\"findId\"/>(int)</td>" +
                    "<td><input name=\"action\" type=\"submit\" value=\"Find\"/></td></tr>");
        out.println("</tbody></table>");
        out.println("<hr/>");
        // form to find all
        out.println("<h3>Find all Users</h3>");
        out.println("<input name=\"action\" type=\"submit\" value=\"FindAll\"/>");
        out.println("<hr/>");
    }
    
    
    private void printHtmlFooter(PrintWriter out) throws IOException {
        out.println("</html>");
        out.println("</body>");
        out.close();
    }
}
