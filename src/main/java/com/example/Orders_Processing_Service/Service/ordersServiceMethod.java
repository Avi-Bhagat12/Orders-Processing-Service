package com.example.Orders_Processing_Service.Service;

import com.example.Orders_Processing_Service.entity.OrderEntity;
import com.example.Orders_Processing_Service.repository.OrderRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ordersServiceMethod {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static OrderRepository orderRepository;

    public ordersServiceMethod(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public static void processOrdersAsync(MultipartFile file) {
        executorService.submit(() -> {
            try (Reader reader = new InputStreamReader(file.getInputStream())) {
                Iterable<CSVRecord> records = CSVFormat.DEFAULT
                        .withHeader("OrderId", "OrderDate", "OrderStatus", "OrderAmount")
                        .withSkipHeaderRecord()
                        .parse(reader);

                for (CSVRecord record : records) {
                    OrderEntity order = new OrderEntity();
                    order.setOrderId(record.get("OrderId"));
                    order.setCustomerId("default-customer"); // You can customize this
                    order.setItemJson("[]"); // Placeholder for items
                    order.setTotal(new BigDecimal(record.get("OrderAmount")));
                    order.setStatus(record.get("OrderStatus"));
                    order.setCreatedAt(Instant.now());
                    order.setUpdatedAt(Instant.now());

                    orderRepository.save(order); // Save to DB
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
