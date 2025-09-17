package com.example.Orders_Processing_Service.Service.externalApi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class InventoryService {
    @Value("${external.inventory.base-url}")
    private String inventoryBaseUrl;

    private final RestTemplate restTemplate;

    public InventoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean reserveStock(String sku, int quantity) {
        String url = inventoryBaseUrl + "/reserve";
        ReservationRequest request = new ReservationRequest(sku, quantity);
        ReservationResponse response =
                restTemplate.postForObject(url, request, ReservationResponse.class);
        return response != null && response.isReserved();
    }

    static class ReservationRequest {
        private String sku;
        private int quantity;
        public ReservationRequest(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }
        public String getSku() { return sku; }
        public int getQuantity() { return quantity; }
    }

    static class ReservationResponse {
        private boolean reserved;
        public boolean isReserved() { return reserved; }
        public void setReserved(boolean reserved) { this.reserved = reserved; }
    }
}

