# ShopSmart — Secure E-Commerce Application

ShopSmart is a full-stack e-commerce project: a Java 17 / Spring Boot REST API with a React (Vite) storefront. It covers customer accounts, product search, carts, checkout, inventory, COD payments, delivery tracking, and role-based administration.

**Backend:** Java 17, Spring Boot, Spring Security, JWT, Spring Data JPA / Hibernate, Bean Validation, MySQL, Maven, JUnit 5, Mockito, OpenAPI / Swagger UI.
**Frontend:** React 19, Vite, Axios, React Router, plain CSS.

## Run locally

1. Install JDK 17 and MySQL 8, and make sure the MySQL service is running.
2. Copy `.env.example` to `.env` in the project root and fill in:
   - `DB_PASSWORD` — your local MySQL password
   - `JWT_SECRET` — a long random string (32+ characters)
   - `ADMIN_EMAIL` / `ADMIN_PASSWORD` — optional first administrator
3. Start the backend from the project root:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Spring Boot imports `.env` automatically (`spring.config.import=optional:file:.env[.properties]`). Real environment variables take precedence, so the same build works in Docker and on Render.

4. Open the API at `http://localhost:8080`, or Swagger UI at `http://localhost:8080/swagger-ui/index.html`.
5. Start the frontend:

   ```powershell
   cd shopsmart-frontend
   npm install
   npm run dev
   ```

   The storefront runs at `http://localhost:5173`.

On the first non-test startup ShopSmart inserts eight sample products if the catalog is empty. Set `shopsmart.demo-data.enabled=false` to disable this.

### Configuration

No credentials are committed. `application.properties` reads everything from the environment, and `.env` is git-ignored.

| Environment variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/shopsmart_db?...` | JDBC URL |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | empty | Database password |
| `JWT_SECRET` | none (required) | JWT signing key, 32+ characters |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (24 h) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | empty | Optional initial administrator |
| `PORT` | `8080` | HTTP port |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated frontend origins allowed by CORS |

The frontend reads `VITE_API_URL` at build time (see `shopsmart-frontend/.env.example`).

`ddl-auto=update` is convenient for local development. For a real deployment, use a dedicated database user and a migration tool such as Flyway.

## API overview

JSON requests use `Content-Type: application/json`. After login, send `Authorization: Bearer <token>` on protected endpoints.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a customer |
| POST | `/api/auth/login` | Public | Sign in and receive a JWT |
| POST | `/api/auth/logout` | Signed in | Invalidate all current JWTs for this account |
| GET | `/api/products?search=mouse&category=Electronics&page=0&size=20&sortBy=name&direction=asc` | Public | Search, filter, page and sort active products |
| GET | `/api/products/categories` | Public | List available categories |
| GET | `/api/products/{id}` | Public | Product details |
| POST, PUT, DELETE | `/api/products[/{id}]` | Admin | Create, update or deactivate a product |
| GET | `/api/cart` | Customer | View own cart and total |
| POST | `/api/cart/items` | Customer | Add `{ "productId": 1, "quantity": 2 }` |
| PUT | `/api/cart/items/{productId}` | Customer | Set quantity with `{ "quantity": 3 }` |
| DELETE | `/api/cart/items/{productId}` | Customer | Remove a product |
| DELETE | `/api/cart` | Customer | Clear own cart |
| POST | `/api/orders` | Customer | Place an order from the cart (`{ "paymentMethod": "COD" }`) |
| GET | `/api/orders` | Customer | Own order history |
| GET | `/api/orders/{id}` | Owner | Own order details |
| POST | `/api/orders/{id}/cancel` | Owner | Cancel a placed, not-yet-shipped order and restore stock |
| GET | `/api/orders/{id}/tracking` | Owner | Delivery status and timeline |
| GET | `/api/payments/order/{orderId}` | Owner | Payment for own order |
| POST | `/api/payments/cod/{orderId}/confirm` | Admin | Mark a COD payment as collected |
| GET | `/api/admin/users` | Admin | Registered customers and admins |
| GET | `/api/admin/orders` | Admin | All orders |
| GET | `/api/admin/products`, `/api/admin/products/{id}` | Admin | All products, including deactivated |
| PATCH | `/api/admin/orders/{orderId}/delivery-status` | Admin | Set `{ "deliveryStatus": "SHIPPED" }` |

### Security behaviour

- Passwords are BCrypt-hashed; JWTs are stateless and carry a token version. Logout increments the version, invalidating every previously issued token for that account.
- Customers can only read their own cart, orders, payments and tracking. Someone else's resource returns **404**, so its existence is not leaked.
- Admin-only actions return **403** for authenticated customers and **401** when no token is present.
- Checkout locks inventory rows and runs in one transaction. Stock is reduced atomically, and a failed checkout leaves the cart untouched.
- A cancelled order restores stock once; a second cancellation, cancelling a shipped order, or confirming a COD payment on a cancelled order returns **409**.
- Order status follows the delivery lifecycle (`PLACED → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`). Customers may cancel until the parcel is shipped.
- Product deletion is a soft deactivation so historical orders stay readable.
- Malformed bodies return **400**, unknown routes **404**, wrong methods **405**, unsupported content types **415**, and genuine server errors **500** (logged server-side without leaking internals).

### Delivery lifecycle

`PROCESSING → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`

`RETURNED` is available as an exceptional final state. The customer tracking endpoint returns the current status plus a completed/current flag for each step.

## Frontend

The React storefront (`shopsmart-frontend`) includes:

- Customer registration and login with JWT stored in `sessionStorage`
- Product catalogue with search-ready API integration
- Cart with quantity updates and removal
- COD checkout and an order confirmation screen
- **My Orders** page with payment status, delivery status and a tracking timeline

The production build is verified with `npm run build` and `npm run lint`.

## Deploy (Render + Aiven MySQL)

1. Create a free Aiven MySQL service and note the host, port, user, password and database name.
2. Push the repository to GitHub.
3. In Render, choose **New → Blueprint** and select the repository. `render.yaml` creates the web service.
4. Fill the environment values:
   - `DB_URL` = `jdbc:mysql://HOST:PORT/DB?useSSL=true&requireSSL=true&serverTimezone=UTC`
   - `DB_USERNAME`, `DB_PASSWORD`
   - `ADMIN_EMAIL`, `ADMIN_PASSWORD`
   - `CORS_ALLOWED_ORIGINS` = your deployed frontend URL (also keep `http://localhost:5173` while testing)
5. Deploy and open `https://<service>.onrender.com/api/products` to confirm it is live.
6. Deploy `shopsmart-frontend` to Vercel or Netlify: build command `npm ci && npm run build`, publish directory `dist`, environment variable `VITE_API_URL=https://<service>.onrender.com`.
7. Add the frontend URL to `CORS_ALLOWED_ORIGINS` on Render and redeploy the backend.

Render's free tier sleeps after inactivity, so the first request can take around 30 seconds.

## Tests

```powershell
.\mvnw.cmd test
```

Integration tests run against an isolated in-memory H2 database and do not need MySQL. They cover registration/login, logout invalidation, product authorization and search, cart quantity rules, checkout and stock checks, cancellation/restocking, order privacy, payment ownership, admin-only COD confirmation, delivery status updates and tracking, plus 400/404/415 error mapping. A Mockito unit test covers product filtering. Current suite: **14 tests passing**.

## Docker (optional)

Copy `.env.example` to `.env`, fill in the values, then:

```powershell
docker compose up --build
```

Docker Compose runs MySQL on host port `3307` and the API on `8080`. The app container receives `DB_PASSWORD` from `MYSQL_ROOT_PASSWORD`.

## Project structure

- `user`, `product`, `cart`, `order`, `payment`: JPA entities and repositories
- `api`: controllers, DTOs, mapping, exception handling, OpenAPI metadata
- `service`: application and transaction logic
- `security`: BCrypt, JWT validation, role authorization, CORS
- `admin`: administrator endpoints and optional first-admin seeding
- `shopsmart-frontend`: React + Vite storefront
- `src/test`: API integration tests and Mockito unit tests

## Known gaps

- **Razorpay is not implemented.** `PaymentMethod.RAZORPAY`, the payment columns and statuses exist, but checkout currently accepts COD only. A production integration needs order creation, checkout.js on the frontend, and server-side signature verification.
- There is no admin UI yet; admin operations are available through Swagger/HTTP.
- Docker image and Render/Aiven deployment configuration exist but have not been verified end-to-end in this environment.
