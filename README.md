🏦 Kestrel Broker – Backend Case Study

A backend API built for a brokerage firm to manage customer assets and orders.
Developed as part of a Java Backend Developer Case Study using Spring Boot 3 and Java 17.

🚀 Tech Stack

Category	Technologies
Language	Java 17
Framework	Spring Boot 3.3
Persistence	Spring Data JPA, H2 (In-memory Database)
Security	Spring Security (Basic Auth)
Validation	Jakarta Validation (@Valid, @NotBlank, etc.)
Documentation	Springdoc OpenAPI 3 / Swagger UI
Testing	JUnit 5, Mockito
Build Tool	Maven
Design Pattern	Strategy Pattern

🧭 Project Overview

The system simulates a simple brokerage backend where customers can:

Create BUY / SELL orders for assets,

View and filter their orders,

Cancel pending orders,

Admins can match orders manually,

All transactions update the customer’s assets and TRY balance automatically.

Each order and asset update is handled with transactional integrity and validation logic.
The project focuses on clean code, domain separation, and secure role-based control.


⚙️ How to Run the Project

1️⃣ Build and Run

You can run it either from terminal or directly in IntelliJ:

./mvnw clean install

./mvnw spring-boot:run

Default port: 8080

2️⃣ API Documentation

Once started, open Swagger UI in your browser:

📄 http://localhost:8080/swagger-ui/index.html

3️⃣ H2 Console (Database)

You can also explore the in-memory database:

📊 http://localhost:8080/h2-console 

Username: sa, password: (empty)

🔒 Authentication

Basic Authentication is used for security.
All requests require authentication.

Roles: 
Admin - (username: admin, password: change-me)

Customer - (username: C1, password: 1234) and (username: C2, password: 1234)

📡 API Endpoints

🟢 Order Controller

Method: POST

Endpoint: /api/orders

Description: Create a new BUY/SELL order

Access: Customer

------------------------------------------

Method: GET

Endpoint: /api/orders

Description: List all orders

Access: Customer/Admin

------------------------------------------

Method: POST

Endpoint: /api/orders/{id}/cancel

Description: Cancel a PENDING order and refund TRY or asset balance

Access: Customer/Admin

------------------------------------------

Method: PUT

Endpoint: /api/orders/{id}/match

Description: Match (execute) an existing order — updates balances and status

Access: Admin

-----------------------------------------

Method: POST

Endpoint: /api/orders/match

Description: Create and immediately match a new order (used for testing/admin purposes)

Access: Admin

----------------------------------------

🟢 Auth Controller

Method: POST

Endpoint: /api/auth/register

Description: Register a new customer account

Access: Public

-----------------------------------------

🟢 Asset Controller

Method: GET

Endpoint: /api/assets

Description: List all assets of the logged-in customer. Admins can view other customers’ assets by providing customerId query param.

Access: Customer / Admin

------------------------------------------

🧩 Business Logic Rules

- TRY (cash balance) is treated as an asset.

- When placing a BUY order, the required TRY amount is reserved.

- When placing a SELL order, the specified asset quantity is reserved.

- Cancelling a pending order refunds the reserved TRY or asset quantity.

- Matching an order finalizes the trade and updates both sides’ balances.

- Orders transition through statuses: PENDING → MATCHED or CANCELED

--------------------------------------------------

🧪 Unit Tests

All business-critical scenarios are covered with JUnit 5 and Mockito.

--------------------------------------------------

🧪 Example Requests

🟩 1️⃣ Create Order

Endpoint: POST /api/orders

Description: Creates a new BUY or SELL order.

Headers: Authorization: Basic base64(username:password)

Example Request Body:

{
  "customerId": "C1",
  "assetName": "AAPL",
  "side": "BUY",
  "price": 100,
  "size": 10
}

Example Response Body:

{
  "id": 12,
  "customerId": "C1",
  "assetName": "AAPL",
  "side": "BUY",
  "price": 100,
  "size": 10,
  "status": "PENDING",
  "createDate": "2025-10-19T14:00:00Z"
}


2️⃣ List Orders

Endpoint: GET /api/orders

Description: Returns a filtered list of orders.
Admins can view all orders; customers see only their own.

Supports filtering by:

customerId (optional, only for admin)

status (PENDING, MATCHED, CANCELED)

from and to date-time range (ISO 8601 format)

Headers: Authorization: Basic base64(username:password)

Query Parameters:

customerId, status, from, to

Example Request:

GET /api/orders?status=PENDING&from=2025-10-01T00:00:00Z&to=2025-10-19T23:59:59Z

Sample Response:

[
  {
    "id": 3,
    "customerId": "C2",
    "assetName": "AAPL",
    "side": "BUY",
    "price": 125,
    "size": 20,
    "status": "MATCHED",
    "createDate": "2025-10-18T09:30:00Z"
  },
  {
    "id": 4,
    "customerId": "C2",
    "assetName": "GOOGL",
    "side": "SELL",
    "price": 300,
    "size": 5,
    "status": "PENDING",
    "createDate": "2025-10-19T10:45:00Z"
  }
]

3️⃣ Cancel Order

Endpoint: POST /api/orders/{orderId}/cancel

Description: Cancels a pending order. Refunds the reserved TRY or asset balance.

Example Request:

POST /api/orders/5/cancel

Example Response:

{
  "id": 5,
  "customerId": "C1",
  "assetName": "AAPL",
  "side": "BUY",
  "status": "CANCELED",
  "price": 100,
  "size": 10
}

4️⃣ Match Existing Order

Endpoint: PUT /api/orders/{id}/match

Description: Matches (executes) an existing order — available to admins only.

Example Request: PUT /api/orders/5/match

Example response:

{
  "id": 5,
  "customerId": "C1",
  "assetName": "AAPL",
  "side": "BUY",
  "status": "MATCHED",
  "price": 100,
  "size": 10
}

5️⃣ Match Immediately (Admin)

Endpoint: POST /api/orders/match

Description: Creates and matches a new order in one step.
Primarily for testing or admin use.

Example Request: 

{
  "customerId": "C1",
  "assetName": "AAPL",
  "side": "SELL",
  "price": 150,
  "size": 5
}


Example Response:

{
  "id": 11,
  "customerId": "C1",
  "assetName": "AAPL",
  "side": "SELL",
  "status": "MATCHED",
  "price": 150,
  "size": 5
}

6️⃣ List Assets

Endpoint: GET /api/assets

Description: Returns all assets belonging to the logged-in user.
Admins can optionally specify customerId query param.

Example Request:

GET /api/assets
Authorization: Basic base64(C1:password)

Example Response:

[
  {
    "assetName": "AAPL",
    "size": 100,
    "usableSize": 100
  },
  {
    "assetName": "TRY",
    "size": 1000000,
    "usableSize": 1000000
  }
]


