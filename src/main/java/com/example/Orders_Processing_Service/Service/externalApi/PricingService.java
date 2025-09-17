package com.example.Orders_Processing_Service.Service.externalApi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class PricingService {
    @Value("${external.pricing.base-url}")
    private String pricingBaseUrl;

    private final RestTemplate restTemplate;

    public PricingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public BigDecimal fetchCurrentPrice(String sku) {
        String url = String.format("%s/items/%s/price", pricingBaseUrl, sku);
        PricingResponse response = restTemplate.getForObject(url, PricingResponse.class);
        return response != null ? response.getPrice() : BigDecimal.ZERO;
    }

    public static class PricingResponse {
        private BigDecimal price;
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
}

