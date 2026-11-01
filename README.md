# ShopSmart — Secure E-Commerce REST API

ShopSmart is a Java 17 / Spring Boot backend for customer accounts, product search, shopping carts, checkout, inventory, and order management. It uses MySQL, Spring Data JPA, Spring Security, BCrypt, JWT, Maven, JUnit 5, Mockito, and OpenAPI/Swagger UI.

## Run locally in IntelliJ

1. Open this Maven project and set **Project SDK** and **Maven Runner JRE** to JDK 17 (`C:\Users\<you>\.jdks\ms-17.0.21.0.2` is one common IntelliJ-managed location).
2. Start MySQL 8 and create database `shopsmart_db`, or allow the configured local MySQL URL to create it.
3. In `ShopSmartApplication` → **Edit Configurations** → **Environment variables**, add `DB_USERNAME` and `DB_PASSWORD` for MySQL. Add `ADMIN_EMAIL` and `ADMIN_PASSWORD` to seed the initial administrator on startup.
4. Run `ShopSmartApplication`. The API is at `http://localhost:8080`.
5. Open the storefront at `http://localhost:8080/` or interactive API docs at `http://localhost:8080/swagger-ui.html`.

On the first non-test startup, ShopSmart adds eight sample products if the catalog is empty. Set `shopsmart.demo-data.enabled=false` to disable this starter data.

Keep credentials in your local environment or IntelliJ run configuration; do not commit them. The built-in JWT key and `ddl-auto=update` are for local learning only. Before deployment, set a strong random `JWT_SECRET`, use a dedicated database account and migration tool, and configure HTTPS and secrets management.

| Environment variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/shopsmart_db?...` | JDBC URL |
| `DB_USERNAME` | `root` | Local MySQL user |
| `DB_PASSWORD` | empty | Local MySQL password |
| `JWT_SECRET` | development key | JWT signing key; replace outside local use |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | empty | Optional initial administrator |

## API overview

JSON requests use `Content-Type: application/json`. After login, send `Authorization: Bearer <token>` for protected endpoints.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a customer |
| POST | `/api/auth/login` | Public | Sign in and receive a JWT |
| POST | `/api/auth/logout` | Signed in | Invalidate all current JWTs for this account |
| GET | `/api/products?search=mouse&category=Electronics&page=0&size=20&sortBy=name&direction=asc` | Public | Search, filter, page, and sort active products |
| GET | `/api/products/categories` | Public | List available product categories |
| GET | `/api/products/{id}` | Public | View product details |
| POST, PUT, DELETE | `/api/products[/{id}]` | Admin | Create, update, or deactivate a product |
| GET | `/api/cart` | Customer | View own cart and total |
| POST | `/api/cart/items` | Customer | Add `{ "productId": 1, "quantity": 2 }` |
| PUT | `/api/cart/items/{productId}` | Customer | Set quantity using `{ "quantity": 3 }` |
| DELETE | `/api/cart/items/{productId}` | Customer | Remove a product |
| DELETE | `/api/cart` | Customer | Clear own cart |
| POST | `/api/orders` | Customer | Place order from cart |
| GET | `/api/orders` | Customer | View own order history |
| GET | `/api/orders/{id}` | Owner | View own order |
| POST | `/api/orders/{id}/cancel` | Owner | Cancel a placed order and restore stock |
| GET | `/api/admin/users`, `/api/admin/orders` | Admin | View customer summaries and all orders |
| GET | `/api/admin/products` | Admin | View all products, including deactivated items |

Customer passwords are BCrypt-hashed. Customers can only access their own carts and orders. Checkout locks inventory rows and runs atomically. Cancelling a placed order restores stock once; a second cancellation returns HTTP 409. Product deletion is a soft deactivation so old orders remain readable. Invalid credentials return HTTP 401 and forbidden actions return HTTP 403.

## Tests

Run in IntelliJ with the Maven `test` goal, or in a terminal configured for JDK 17:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-17.0.21.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

The integration tests use an isolated in-memory H2 database and do not require a running MySQL server. They cover registration, login, product authorization/search, cart quantity changes, checkout, stock checks, cancellation/restocking, logout, and access control. A Mockito unit test covers product filtering.

## Docker (optional)

Copy `.env.example` to `.env`, replace each placeholder with local values, then run:

```powershell
docker compose up --build
```

The API is available at `http://localhost:8080`; Swagger UI is at `/swagger-ui.html`. To stop the app while preserving the local database volume, run `docker compose down`.

## Project structure

- `user`, `product`, `cart`, and `order`: JPA entities and repositories
- `api`: controllers, request/response DTOs, mapping, exception handling, OpenAPI metadata
- `service`: application and transaction logic
- `security`: BCrypt, JWT validation, and role authorization
- `admin`: administrator endpoints and optional first-admin initialization
- `src/test`: API integration tests and Mockito unit tests
