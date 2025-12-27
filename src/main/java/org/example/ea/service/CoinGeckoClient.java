package org.example.ea.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.example.ea.model.CoinMarket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoinGeckoClient {
    private static final Logger log = LoggerFactory.getLogger(CoinGeckoClient.class);
    private static final String BASE_URL = "https://api.coingecko.com/api/v3/coins/markets";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<CoinMarket> fetchTopMarkets(String currency) {
        String normalized = currency == null ? "usd" : currency.toLowerCase(Locale.ROOT);
        String query = BASE_URL + "?vs_currency=" + normalized
                + "&order=market_cap_desc&per_page=10&page=1&sparkline=true&price_change_percentage=24h";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(query))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        Instant started = Instant.now();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long durationMs = Duration.between(started, Instant.now()).toMillis();
            if (response.statusCode() >= 400 || response.body() == null) {
                log.warn("coingecko error status={} durationMs={} currency={}", 
                        response.statusCode(), durationMs, normalized);
                return Collections.emptyList();
            }
            List<CoinMarket> markets = mapper.readValue(response.body(), new TypeReference<>() {});
            if (markets.isEmpty()) {
                log.warn("coingecko empty list currency={}", normalized);
                return Collections.emptyList();
            }
            String coinsList = markets.stream()
                    .map(CoinMarket::symbol)
                    .map(String::toUpperCase)
                    .collect(Collectors.joining(", "));
            log.info("coingecko ts={} currency={} coins=[{}] durationMs={}",
                    Instant.now(), normalized.toUpperCase(), coinsList, durationMs);
            return markets;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("coingecko interrupted currency={}", normalized, e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("coingecko error currency={}", normalized, e);
            return Collections.emptyList();
        }
    }
}

