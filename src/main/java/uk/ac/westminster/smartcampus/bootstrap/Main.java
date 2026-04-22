package uk.ac.westminster.smartcampus.bootstrap;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import uk.ac.westminster.smartcampus.config.SmartCampusApplication;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("smartcampus.port", "8080"));

        Server server = new Server(port);
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(new ServletContainer(
                ResourceConfig.forApplication(new SmartCampusApplication()))), "/*");

        server.setHandler(contextHandler);
        server.start();
        server.join();
    }
}
