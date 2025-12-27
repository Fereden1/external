package org.example.ea.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.example.ea.service.CoinGeckoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(value = "/api/coins", loadOnStartup = 1)
public class CryptoApiServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CryptoApiServlet.class);
    private final CoinGeckoClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> currencies = List.of("usd", "eur", "rub");

    public CryptoApiServlet() {
        this(new CoinGeckoClient());
    }

    public CryptoApiServlet(CoinGeckoClient client) {
        this.client = client;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        String currency = resolveCurrency(req.getParameter("currency"));
        log.info("Запрос данных для валюты: {}", currency);
        var coins = client.fetchTopMarkets(currency);
        if (coins.isEmpty()) {
            mapper.writeValue(resp.getWriter(), Collections.emptyList());
            return;
        }
        log.info("Возвращено {} монет для валюты: {}", coins.size(), currency);
        mapper.writeValue(resp.getWriter(), coins);
    }

    private String resolveCurrency(String value) {
        if (value == null) {
            return "usd";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return currencies.contains(normalized) ? normalized : "usd";
    }
}

