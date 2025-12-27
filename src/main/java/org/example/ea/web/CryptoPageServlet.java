package org.example.ea.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.example.ea.service.CoinGeckoClient;
import org.example.ea.view.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(value = "/", loadOnStartup = 1)
public class CryptoPageServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CryptoPageServlet.class);
    private final CoinGeckoClient client;
    private final TemplateRenderer renderer;
    private final List<String> currencies = List.of("usd", "eur", "rub");

    public CryptoPageServlet() {
        this(new CoinGeckoClient(), new TemplateRenderer());
    }

    public CryptoPageServlet(CoinGeckoClient client, TemplateRenderer renderer) {
        this.client = client;
        this.renderer = renderer;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");
        String currency = resolveCurrency(req.getParameter("currency"));
        log.info("Загрузка страницы для валюты: {}", currency);
        List<?> coins;
        try {
            coins = client.fetchTopMarkets(currency);
        } catch (Exception e) {
            log.error("Ошибка при загрузке данных для страницы, валюта: {}", currency, e);
            coins = Collections.emptyList();
        }
        renderer.render("coins.ftl", Map.of(
                "coins", coins,
                "currency", currency,
                "currencies", currencies,
                "ctx", req.getContextPath()
        ), resp.getWriter());
    }

    private String resolveCurrency(String value) {
        if (value == null) {
            return "usd";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return currencies.contains(normalized) ? normalized : "usd";
    }
}

