package org.example.ea.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinMarket(
        String id,
        String name,
        String symbol,
        @JsonProperty("current_price") Double currentPrice,
        @JsonProperty("price_change_percentage_24h") Double priceChangePercentage24h,
        @JsonProperty("market_cap") Double marketCap,
        @JsonProperty("sparkline_in_7d") Sparkline sparklineIn7d
) {
    @JsonGetter("sparkline")
    public List<Double> getSparkline() {
        if (sparklineIn7d != null && sparklineIn7d.prices() != null) {
            return sparklineIn7d.prices();
        }
        return List.of();
    }

    public record Sparkline(@JsonProperty("price") List<Double> prices) {
    }
}

