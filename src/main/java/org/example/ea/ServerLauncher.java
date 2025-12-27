package org.example.ea;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.example.ea.service.CoinGeckoClient;
import org.example.ea.view.TemplateRenderer;
import org.example.ea.web.CryptoApiServlet;
import org.example.ea.web.CryptoPageServlet;

public class ServerLauncher {
    public static void main(String[] args) throws Exception {
        CoinGeckoClient client = new CoinGeckoClient();
        TemplateRenderer renderer = new TemplateRenderer();
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new CryptoPageServlet(client, renderer)), "/");
        context.addServlet(new ServletHolder(new CryptoApiServlet(client)), "/api/coins");
        server.setHandler(context);
        server.start();
        server.join();
    }
}

