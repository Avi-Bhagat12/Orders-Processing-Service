Orders Processing Service 

1. Purpose
The service manages customer orders through REST endpoints and provides several key features.
It allows the creation of single orders using JSON input and supports bulk creation of orders by uploading a CSV file, which is processed asynchronously.
Users can retrieve specific order details by providing a unique orderId, and the application also integrates with external Pricing and Inventory APIs to fetch current item prices and reserve stock as part of the order-processing workflow.
2. Technology Stack
The application is built on a modern technology stack designed for scalability and ease of development. It uses Spring Boot 3+ as the core application framework and relies on Spring Web along with Jakarta Validation to handle REST endpoints and input validation.
For data access, the project integrates Spring Data JPA, with H2 serving as the in-memory database during development. CSV files are parsed using Apache Commons CSV, while asynchronous processing is handled through Java’s ExecutorService.
External API calls are made using Spring RestTemplate, and the entire project is built and managed using Maven.
Key Packages
The project follows a clear layered structure.
The controller layer exposes the REST API endpoints that handle client requests and responses.
The service layer contains the core business logic and manages all interactions with external systems such as the Pricing and Inventory APIs.
The entity layer defines the database schema representation, mapping Java classes to database tables.
The repository layer provides the data persistence functionality using Spring Data JPA for seamless database operations.
Finally, the model layer holds the API data transfer objects (DTOs) used to pass information between the client, controller, and service layers.


4. Configuration
application.yml 
In this Project I am using a in memory H2 database only functional when the application is running.
 
For Production switch:
When we move to a real database (e.g. PostgreSQL) we only change this block:
spring:
datasource:
url: jdbc:postgresql://localhost:5432/myordersdb
username: my_pg_user
password: strongpassword
jpa:
hibernate:
ddl-auto: update
Also add the PostgreSQL JDBC driver to your pom.xml.




5. Component-Level Functionality
5.1 OrderEntity
The OrderEntity class represents the database table for storing order information and is annotated with @Entity and @Table(name = "orders") to map it to the orders table.
It includes fields such as id, orderId, customerId, itemJson, total, status, createdAt, and updatedAt, which capture all essential details of an order.
The corresponding OrderRepository interface extends JpaRepository, providing standard CRUD operations and adding a custom query method Optional<OrderEntity> findByOrderId(String orderId) to retrieve orders by their unique orderId.
5.2 Models
The Item class is a simple Plain Old Java Object (POJO) that represents a single product line in an order, holding details such as a unique SKU identifier, the quantity ordered, and an optional price.
The OrderRequest class serves as the input Data Transfer Object (DTO), containing the customerId along with a list of Item objects to capture the information needed when a client submits a new order.
The OrderResponse class acts as the output DTO, defining the fields that are returned to clients after an order is processed, ensuring that only the necessary and appropriate data is exposed.
5.3 Services
he OrdersServiceMethod handles CSV uploads asynchronously by using a Java ExecutorService. It reads each row of the uploaded CSV file with Apache Commons CSV, converts the data into OrderEntity objects, and saves them to the database.
The PricingService reads its base URL from the application.yml configuration and uses an HTTP client to fetch real-time prices for specific SKUs before an order is confirmed.
Similarly, the InventoryService communicates with an external inventory API to reserve stock, ensuring that the required items are available before the order is finalized.







5.4 Controller
The OrdersController, mapped to the base path /orders, exposes the REST endpoints for managing orders.
The POST /orders endpoint accepts an OrderRequest payload and, for each item in the request, calls the PricingService to fetch real-time prices and the InventoryService to reserve stock. It then persists the order as an OrderEntity in the database and returns an OrderResponse.
The POST /orders/batch-upload endpoint accepts a CSV file as a MultipartFile, validates that the file type is correct, and delegates the asynchronous processing of the file to the OrdersServiceMethod.
Finally, the GET /orders/{orderId} endpoint retrieves and returns the order details in JSON format if the specified orderId exists, or responds with a 404 status code if the order is not found.
6. Execution Flow
6.1 Single Order Creation (POST Method)
For single order creation using the POST /orders method, the client first sends an HTTP POST request to /orders with a JSON payload containing the order details. The OrdersController validates the input and then delegates processing to the service layer. In the service layer, each item in the order is processed by calling the PricingService to retrieve the latest price and the InventoryService to reserve the required quantity. After fetching prices and reserving stock, the service calculates the total cost of the order, sets the appropriate order status, maps the data to an OrderEntity, and persists it to the database through the OrderRepository. Finally, the service returns an OrderResponse to the client, including key information such as the generated orderId, the order status, and the total amount.










6.2 Batch Upload (POST Method)
When a client uploads a CSV file to the POST /orders/batch-upload endpoint, the OrdersController first validates that the file is present and has a .csv extension. After validation, the request is passed to the OrdersServiceMethod, which processes the file asynchronously. Using Apache Commons CSV, the service parses the uploaded file, expecting headers such as OrderId, OrderDate, OrderStatus, and OrderAmount. For each record, it creates a new OrderEntity, assigns a placeholder customerId and itemJson, and then saves the entity to the database. This asynchronous processing allows the upload request to return quickly while the records are being stored in the background.
Example: http://localhost:8080/orders/batch-upload

 







6.3 Fetch Order (GET Method)
When a client calls the GET /orders/{orderId} endpoint, the controller first invokes orderRepository.findByOrderId to search for the order in the database. If the order is found, the system responds with a 200 OK status along with the order details in JSON format. If the order does not exist, it returns a 404 Not Found response.

Example: http://localhost:8080/orders/ORD-1001


 







7. External API Integration
Pricing API
For every order item, the application calls the external pricing service to fetch the current price.
The request is sent to an endpoint such as GET {pricingBaseUrl}/price?sku=LAPTOP-001, where pricingBaseUrl is the base URL defined in the application’s configuration.
This base URL is injected into the PricingService class using Spring’s @Value annotation,
for example:
@Value("${external.pricing.base-url}")
private String pricingBaseUrl;
This setup allows the service to dynamically read the correct pricing API URL from application.yml (or other environment-specific configuration) without hard-coding it in the Java code.

Inventory API
The Inventory API is used to reserve or verify stock availability for ordered items by sending a request such as POST {inventoryBaseUrl}/reserve.
The base URL for this external service is injected into the InventoryService class in the same way as the pricing service, using Spring’s @Value("${external.inventory.base-url}") annotation to read the value from the application configuration.
Because this is an external dependency, the service can be easily mocked in development or testing environments, allowing the application to simulate inventory checks or reservations without relying on the live inventory system.

8. Error Handling & Validation
The controller responds with an HTTP 400 Bad Request status if the client uploads an invalid CSV file or sends malformed JSON data.
Meanwhile, the service that processes the CSV logs any parsing or data-handling errors internally, but these failures do not block the overall upload request—allowing the endpoint to acknowledge receipt of the file while problematic records are reported separately.

