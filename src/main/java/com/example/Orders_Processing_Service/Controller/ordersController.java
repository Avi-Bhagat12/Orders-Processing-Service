package com.example.Orders_Processing_Service.Controller;

import com.example.Orders_Processing_Service.Service.externalApi.InventoryService;
import com.example.Orders_Processing_Service.Service.externalApi.PricingService;
import com.example.Orders_Processing_Service.Service.ordersServiceMethod;
import com.example.Orders_Processing_Service.entity.OrderEntity;
import com.example.Orders_Processing_Service.model.item.Item;
import com.example.Orders_Processing_Service.repository.OrderRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("orders")
public class ordersController {

    private final ordersServiceMethod orderService;
    private final OrderRepository orderRepository;
    private final PricingService pricingService;
    private final InventoryService inventoryService;

    public ordersController(ordersServiceMethod orderService,
                            OrderRepository orderRepository,
                            PricingService pricingService,
                            InventoryService inventoryService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable String orderId) {
        return orderRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrders(@RequestBody OrderEntity orderDetails) {
        try {
            List<Item> items = parseItems(orderDetails.getItemJson());
            BigDecimal totalPrice = BigDecimal.ZERO;

            for (Item item : items) {
                // Fetch latest price
                BigDecimal price = pricingService.fetchCurrentPrice(item.getSku());
                totalPrice = totalPrice.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));

                // Reserve stock
                boolean reserved = inventoryService.reserveStock(item.getSku(), item.getQuantity());
                if (!reserved) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body("Item " + item.getSku() + " is out of stock.");
                }
            }

            orderDetails.setTotal(totalPrice);
            orderDetails.setCreatedAt(Instant.now());
            orderDetails.setUpdatedAt(Instant.now());

            OrderEntity savedOrder = orderRepository.save(orderDetails);
            return ResponseEntity.ok(savedOrder);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Invalid itemJson format.");
        }
    }

    @PostMapping("/batch-upload")
    public ResponseEntity<String> uploadOrders(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Invalid file. Please upload a CSV file.");
        }
        orderService.processOrdersAsync(file);
        return ResponseEntity.ok("File uploaded successfully. Orders are being processed.");
    }

    private List<Item> parseItems(String itemJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(itemJson, new TypeReference<List<Item>>() {});
    }
}