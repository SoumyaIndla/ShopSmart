# ShopSmart — Complete Project Guide

> A from-scratch explanation of the project, written for a fresher.
> Read this once end-to-end; then use the Interview Q&A section to practise.

---

## 1. What is ShopSmart? (60-second pitch)

ShopSmart is a **full-stack e-commerce web application**.

- Customers can register, log in, browse and search products, filter by category, sort by price, view product details, add items to a cart, place an order with **Cash on Delivery** or **Razorpay (UPI/Card)**, see their order history, track delivery, and cancel an order before it ships.
- Admins get a **separate console** to see live orders, update delivery status, confirm COD payments, and add/edit/deactivate products.

**Architecture in one line:** a React storefront talks to a Spring Boot REST API secured with JWT, which stores data in a MySQL-compatible database (TiDB Cloud), and integrates Razorpay for online payments.

**Live demo:** <https://shopsmart-gaj7.onrender.com>  
**Admin console:** <https://shopsmart-gaj7.onrender.com/admin>  
**Admin login:** `admin@shopsmart.com` + the `ADMIN_PASSWORD` configured in the environment.

---

## 2. Technology stack and why

| Layer | Technology | Why it was chosen |
|---|---|---|
| Language | Java 17 | Strong typing, industry-standard for backend roles |
| Backend framework | Spring Boot 4 | Auto-configuration, embedded Tomcat, production-ready REST |
| Security | Spring Security + JWT | Stateless authentication, role-based authorization |
| Persistence | Spring Data JPA + Hibernate | Less boilerplate, object-relational mapping |
| Database (local) | MySQL 8 | Common relational DB, easy to run |
| Database (live) | TiDB Cloud Starter (MySQL-compatible) | Free, no expiry, 5 GiB storage, MySQL protocol |
| Validation | Jakarta Bean Validation | Declarative request validation |
| API docs | springdoc OpenAPI + Swagger UI | Interactive API testing |
| Build | Maven | Standard Java build tool |
| Frontend | React 19 + Vite | Component-based UI, fast dev server |
| HTTP client | Axios | Simple API calls and interceptors |
| Payments | Razorpay Java SDK + Checkout.js | Indian market, test mode available |
| Deployment | Render (Docker) + TiDB Cloud | Free tier for a portfolio project |

---

## 3. Architecture

```
Browser (React SPA)
   │  HTTPS
   ▼
Render Web Service (Spring Boot, Docker, port 10000)
   ├── serves the built React files from src/main/resources/static
   ├── /api/**  → REST controllers → services → repositories
   ├── /admin   → forwards to index.html (React handles the route)
   └── /swagger-ui/** → API documentation
   │  JDBC + TLS
   ▼
TiDB Cloud (MySQL-compatible) — shopsmart database
```

External services:

```
Browser → Razorpay Checkout popup → Razorpay servers
Browser → your backend /api/payments/razorpay/verify (signature check)
```

**Important design decision:** the React build is copied into Spring Boot's `static` folder, so **one Render service** serves both the UI and the API. This removes CORS issues and halves the deployment work.

---

## 4. Project structure

```
ShopSmart/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── render.yaml
├── .env.example
├── src/main/java/com/shopsmart/
│   ├── ShopSmartApplication.java
│   ├── api/          controllers, DTOs, mapper, exception handler, OpenAPI
│   ├── security/     SecurityConfig, JWT filter, JWT service
│   ├── service/      business logic (auth, cart, order, product, payment)
│   ├── user/         ShopUser entity, Role, repository
│   ├── product/      Product entity, repository, demo seed
│   ├── cart/         CartItem entity, repository
│   ├── order/        CustomerOrder, OrderLine, statuses, repository
│   ├── payment/      Payment, method/status, repository, RazorpayService
│   ├── admin/        AdminController, admin seed
│   └── controller/   PaymentController
├── src/main/resources/
│   ├── application.properties
│   └── static/       built React app (index.html, assets, placeholder.svg)
├── src/test/         16 automated tests (H2)
└── shopsmart-frontend/
    └── src/
        ├── App.jsx, Auth.jsx, Orders.jsx, ProductDetail.jsx
        ├── Admin.jsx
        └── services/api.js
```

---

## 5. Database design

### Tables

**users**
| Column | Meaning |
|---|---|
| id | primary key |
| full_name | customer/admin name |
| email | unique login |
| password | BCrypt hash (never plain text) |
| role | CUSTOMER or ADMIN |
| token_version | incremented on logout to invalidate old JWTs |
| created_at | registration time |

**products**
| Column | Meaning |
|---|---|
| id, name, description, price, stock, category | product data |
| image_url | external image URL |
| active | soft delete flag (false = hidden, not deleted) |
| created_at | creation time |

**cart_items**
| Column | Meaning |
|---|---|
| id | primary key |
| user_id → users | owner |
| product_id → products | item |
| quantity | count |
| added_at | ordering in cart |
| — | unique (user_id, product_id): one row per product per cart |

**customer_orders**
| Column | Meaning |
|---|---|
| id, user_id, total, created_at | order header |
| status | PLACED, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, RETURNED, CANCELLED |
| delivery_status | PROCESSING, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, RETURNED |

**order_items**
| Column | Meaning |
|---|---|
| id, order_id, product_id, quantity | line item |
| unit_price | price **captured at checkout** (so later price changes do not alter old orders) |

**payments**
| Column | Meaning |
|---|---|
| id, order_id (unique), amount | one payment per order |
| method | COD or RAZORPAY |
| status | PENDING, SUCCESS, FAILED, REFUNDED |
| razorpay_order_id / razorpay_payment_id | gateway ids |
| created_at / updated_at | audit fields |

### Relationships

```
User 1 ─── N CustomerOrder 1 ─── N OrderLine N ─── 1 Product
User 1 ─── N CartItem     N ─── 1 Product
CustomerOrder 1 ─── 1 Payment
```

---

## 6. Features implemented

### Customer

- Register with name, email, password (validated).
- Login and receive a JWT.
- Logout (invalidates all existing tokens for that user).
- Browse products with pagination.
- Search by name.
- Filter by category.
- Sort by name, price (low/high), category, stock.
- Open a product details modal with description, price, stock and image.
- Add to cart, update quantity, remove item, clear cart.
- “Added to cart → View Cart” toast.
- Checkout with **Cash on Delivery**.
- Checkout with **Razorpay** (UPI / card / netbanking test mode).
- View order history and order details.
- View payment method and payment status per order.
- Track delivery with a status timeline.
- Cancel an order until it is shipped (stock is restored).

### Admin

- Separate `/admin` console with its own login.
- Live order dashboard (polls every 10 seconds).
- Customer name and email on each order.
- Payment status badges (SUCCESS / PENDING / FAILED).
- Change delivery status with a dropdown; order status follows automatically.
- Confirm COD payment.
- View all products including deactivated ones.
- Add a product (name, description, price, stock, category, image URL with preview).
- Edit a product (pre-filled form, PUT).
- Deactivate a product (soft delete).

### Backend / platform

- JWT authentication filter on every request.
- Role-based authorization (`CUSTOMER`, `ADMIN`).
- Customer ownership checks on carts, orders, payments and tracking.
- Transactional checkout with stock validation.
- Pessimistic row locks so two customers cannot buy the last item.
- Global exception handler with correct HTTP codes.
- Bean validation on request bodies.
- Swagger UI for testing.
- 16 automated tests.
- Docker image + Render deployment.
- TiDB Cloud database.

---

## 7. End-to-end flows

### 7.1 Registration and login

```
POST /api/auth/register
 → validate name/email/password
 → check email uniqueness
 → BCrypt-encode password
 → save user with role CUSTOMER
 → generate JWT (subject=email, role, tokenVersion)
 → return token

POST /api/auth/login
 → Spring AuthenticationManager checks email + BCrypt password
 → generate JWT
 → return token
```

The JWT is sent on later requests as:

```
Authorization: Bearer <token>
```

### 7.2 JWT validation (every protected request)

```
JwtAuthenticationFilter
 → read Authorization header
 → verify signature + expiry with the secret
 → read email and tokenVersion from the token
 → load user from DB
 → compare tokenVersion with the user's current version
 → if match, set Authentication in SecurityContext
 → otherwise leave request unauthenticated (→ 401)
```

This is why logout works: logout increments `token_version`, so every token issued earlier becomes invalid.

### 7.3 Browsing and cart

```
GET /api/products?search=&category=&sortBy=price&direction=asc
GET /api/products/{id}
POST /api/cart/items            { productId, quantity }
PUT /api/cart/items/{productId} { quantity }
DELETE /api/cart/items/{productId}
DELETE /api/cart
```

Cart add/update checks:
- product exists and is active,
- requested quantity ≤ stock.

### 7.4 Checkout (COD or Razorpay order creation)

```
POST /api/orders { paymentMethod: "COD" | "RAZORPAY" }
```

Inside one transaction:

1. Load the user's cart; reject if empty.
2. For each item, lock the product row (`SELECT ... FOR UPDATE`).
3. Verify the product is still active and has enough stock.
4. Reduce stock.
5. Create `OrderLine` with the current price.
6. Create `CustomerOrder` with total and status `PLACED`.
7. Create `Payment` with `PENDING`.
8. Save order + payment.
9. Delete cart items.
10. Commit.

If anything fails (e.g., insufficient stock), the transaction rolls back, the cart stays, and stock is unchanged.

### 7.5 Razorpay payment

```
1. POST /api/orders { paymentMethod: "RAZORPAY" }   → order + PENDING payment
2. POST /api/payments/razorpay/order/{orderId}
   → backend creates a Razorpay order (amount in paise)
   → returns keyId + razorpayOrderId + amount
3. Frontend opens Razorpay Checkout with that order id
4. Customer pays
5. Razorpay calls the frontend handler with:
   razorpay_order_id, razorpay_payment_id, razorpay_signature
6. Frontend POSTs them to /api/payments/razorpay/verify
7. Backend computes HMAC-SHA256(orderId + "|" + paymentId, keySecret)
   and compares it with the signature
8. If valid → payment SUCCESS; else 400
```

**Security point:** the amount is created on the server, and the signature is verified on the server. The browser can never mark a payment as successful by itself.

> Limitation: verification depends on the browser calling `/verify`. A production system should also use a Razorpay **webhook** (`payment.captured`) so payment status is reliable even if the user closes the tab.

### 7.6 Order history, tracking and cancellation

```
GET  /api/orders                 → own orders
GET  /api/orders/{id}            → own order detail
GET  /api/orders/{id}/tracking   → delivery status + timeline
POST /api/orders/{id}/cancel     → cancel if not shipped
```

Cancel rules:
- cannot cancel an already cancelled order,
- cannot cancel a shipped/out-for-delivery/delivered/returned order,
- stock is restored exactly once.

Tracking timeline is derived from the current `delivery_status` enum: steps before the current one are marked completed.

### 7.7 Admin delivery update

```
PATCH /api/admin/orders/{orderId}/delivery-status
Body: { "deliveryStatus": "SHIPPED" }
```

The backend:
- loads the order,
- updates `delivery_status`,
- syncs `status` (unless the order is cancelled),
- returns the updated order.

The admin dashboard polls `GET /api/admin/orders` every 10 seconds, so new orders, cancellations and payment changes appear automatically.

---

## 8. API summary

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register customer |
| POST | `/api/auth/login` | Public | Login, get JWT |
| POST | `/api/auth/logout` | Logged in | Invalidate all tokens |
| GET | `/api/products` | Public | Search/filter/sort/page |
| GET | `/api/products/categories` | Public | Categories |
| GET | `/api/products/{id}` | Public | Product detail |
| POST/PUT/DELETE | `/api/products[/{id}]` | Admin | Create/update/deactivate |
| GET | `/api/cart` | Customer | View cart |
| POST | `/api/cart/items` | Customer | Add item |
| PUT | `/api/cart/items/{productId}` | Customer | Set quantity |
| DELETE | `/api/cart/items/{productId}` | Customer | Remove |
| DELETE | `/api/cart` | Customer | Clear |
| POST | `/api/orders` | Customer | Checkout |
| GET | `/api/orders` | Customer | Order history |
| GET | `/api/orders/{id}` | Owner | Order detail |
| GET | `/api/orders/{id}/tracking` | Owner | Delivery timeline |
| POST | `/api/orders/{id}/cancel` | Owner | Cancel + restock |
| GET | `/api/payments/order/{orderId}` | Owner | Payment details |
| POST | `/api/payments/cod/{orderId}/confirm` | Admin | Confirm COD |
| POST | `/api/payments/razorpay/order/{orderId}` | Owner | Create Razorpay order |
| POST | `/api/payments/razorpay/verify` | Owner | Verify signature |
| GET | `/api/admin/users` | Admin | List users |
| GET | `/api/admin/orders` | Admin | All orders |
| GET | `/api/admin/products` | Admin | All products |
| PATCH | `/api/admin/orders/{orderId}/delivery-status` | Admin | Update delivery |

---

## 9. Security implemented

| Control | Where / how |
|---|---|
| Password hashing | BCrypt (`BCryptPasswordEncoder`) |
| JWT signing | HMAC with a 32+ character secret from env |
| Stateless sessions | `SessionCreationPolicy.STATELESS` |
| Token invalidation | `token_version` claim compared on every request; logout increments it |
| Role-based access | `@PreAuthorize("hasRole('ADMIN')")` + URL rules |
| Ownership checks | cart/orders/payments queried by `email`; another user's resource returns **404**, not 403, so existence is not leaked |
| Input validation | `@NotBlank`, `@Email`, `@Size`, `@Min`, `@DecimalMin` |
| SQL injection | Spring Data JPA parameter binding; no string-concatenated SQL |
| Mass assignment | DTOs (`RegisterRequest`, `ProductRequest`, …) instead of binding entities |
| CORS | Configurable allow-list (`CORS_ALLOWED_ORIGINS`) |
| Secrets | `.env` locally, env vars on Render; `.env` is git-ignored |
| Error leakage | Generic 500 message to client; real stack trace logged server-side |
| HTTPS | Render provides TLS |
| Payment integrity | Amount created server-side; Razorpay signature verified server-side |
| Card data | Never stored; Razorpay Checkout handles it |
| Soft delete | Products are deactivated, so historical orders stay readable |
| Stock safety | Checkout is transactional and uses row locks |

### 9.1 Security gaps (be honest in interviews)

- JWT is stored in `sessionStorage` → readable by JavaScript, so an XSS bug could steal it. httpOnly cookies + refresh tokens would be stronger.
- No refresh-token rotation.
- No login rate limiting / account lockout.
- No email verification or password reset.
- No audit log for admin actions.
- Database user is the TiDB **root** user; production should use a least-privilege user.
- `ddl-auto=update` instead of migrations (Flyway/Liquibase).
- No webhook for Razorpay, so payment verification depends on the browser.
- No backups configured on the free database.
- CSRF: the API uses a bearer header, not cookies, so classic CSRF is not the main risk; XSS is.

---

## 10. Features NOT implemented

### Business features

- Product reviews and ratings.
- Wishlist.
- Coupons / discount codes.
- Multiple product images and variants (size/colour).
- File upload for images (currently image URL only).
- Shipping charges, tax, multi-currency.
- Email/SMS order notifications.
- Refunds and returns workflow (statuses exist, flow does not).
- Invoice PDF.
- Admin UI for customer management (backend `/api/admin/users` exists, UI does not show it).
- Pagination for the order list.
- Delivery history with timestamps (timeline is derived, not stored).

### Technical / production features

- Razorpay **webhook** and live-mode KYC.
- Refresh tokens / httpOnly cookies.
- Rate limiting / account lockout.
- Password reset / email verification.
- Database migrations (Flyway).
- CI/CD pipeline (GitHub Actions).
- Centralised logging / monitoring / alerts.
- Backups and disaster recovery.
- Custom domain.
- Load testing and caching (Redis).
- Frontend unit/E2E tests.

---

## 11. Testing

**16 automated tests** (JUnit 5 + MockMvc + H2 + Mockito):

- Registration, login, logout token invalidation.
- Wrong password → 401.
- Product search/filter/pagination.
- Customer cannot create a product → 403.
- Cart add/update/remove and stock limits.
- Checkout, stock reduction, cart clearing.
- Insufficient stock → 409, cart preserved.
- Cancellation and stock restoration.
- Order privacy (another user gets 404).
- COD payment ownership and admin-only confirmation.
- Razorpay signature success and bad-signature rejection.
- Delivery status update + tracking.
- 400/404/415 error mapping.

Commands:

```powershell
cd C:\Users\soumy\IdeaProjects\ShopSmart
.\mvnw.cmd test
```

Frontend checks:

```powershell
cd shopsmart-frontend
npm run lint
npm run build
```

---

## 12. Running locally

### Prerequisites

- JDK 17
- MySQL 8 running locally
- Node.js 20+
- Maven wrapper (included)

### Backend

1. Create `.env` in the project root:
```
DB_PASSWORD=<your local MySQL password>
JWT_SECRET=<32+ random characters>
ADMIN_EMAIL=admin@shopsmart.com
ADMIN_PASSWORD=<your admin password>
RAZORPAY_KEY_ID=<test key id>
RAZORPAY_KEY_SECRET=<test key secret>
```
2. Run:
```powershell
cd C:\Users\soumy\IdeaProjects\ShopSmart
.\mvnw.cmd spring-boot:run
```
The API is at `http://localhost:8080`, Swagger at `http://localhost:8080/swagger-ui/index.html`.

### Frontend

```powershell
cd shopsmart-frontend
npm install
npm run dev
```
Storefront: `http://localhost:5173`  
Admin: `http://localhost:5173/admin`

> In dev the frontend calls `http://localhost:8080`. In the production build it uses relative URLs, because Spring Boot serves it on the same origin.

---

## 13. Deployment

### Current live setup

- **Render** free web service, Docker, Singapore: `https://shopsmart-gaj7.onrender.com`
- **TiDB Cloud Starter** (MySQL-compatible), database `shopsmart`
- One service serves the React app + API; `/admin` is forwarded to `index.html`.

### Environment variables on Render

```
DB_URL=jdbc:mysql://HOST:4000/shopsmart?sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=<tidb user>
DB_PASSWORD=<tidb password>
JWT_SECRET=<random secret>
ADMIN_EMAIL=admin@shopsmart.com
ADMIN_PASSWORD=<admin password>
RAZORPAY_KEY_ID=<razorpay key id>
RAZORPAY_KEY_SECRET=<razorpay key secret>
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Free-tier limits

- **Render:** 750 instance hours/month. Free service sleeps after 15 min idle; next request takes ~1 minute to wake. Add an UptimeRobot monitor on `/api/products` every 5 minutes to keep it warm.
- **TiDB Starter:** 5 GiB row storage, 50M request units/month, no expiry. Quota resets monthly; data is not deleted when idle.

---

## 14. Hard bugs solved (great interview stories)

1. **Swagger showed “Authorized” but sent no token.**  
   The OpenAPI spec had a security scheme but no operation required it. Adding `@SecurityRequirement` and restarting fixed it. Lesson: “Authorized” in Swagger UI does not mean every endpoint uses it.

2. **Order history returned 500.**  
   `spring.jpa.open-in-view=false` + lazy `items`/`payment` + mapping outside a transaction → `LazyInitializationException`. Fixed with `@EntityGraph` fetch joins.

3. **Admin delivery update returned 500 on MySQL.**  
   The `status` column was an old `ENUM('PLACED','CANCELLED')`. Writing `SHIPPED` truncated. Fixed with `ALTER TABLE ... MODIFY status VARCHAR(30)`.

4. **TiDB rejected checkout and cancel.**  
   Hibernate generated `FOR UPDATE OF alias`, which MySQL accepts but TiDB does not. Replaced those locked queries with native `SELECT ... FOR UPDATE`. This is a good example of database-portability thinking.

5. **Render instance stuck after sleep.**  
   The container was unresponsive after a cold start. A fresh deploy fixed it; a keep-alive monitor prevents recurrence.

6. **Render static-site API ignored build settings.**  
   The frontend was instead bundled into Spring Boot, giving one service, same-origin API and a simpler deployment.

---

## 15. Interview Q&A

### Q1. Explain the project in one minute.
ShopSmart is a full-stack e-commerce app. React frontend, Spring Boot REST backend, Spring Security with JWT, MySQL/TiDB database. Customers browse products, manage a cart, check out with COD or Razorpay, view orders, track delivery and cancel before shipping. Admins use a separate dashboard to manage products, orders, COD confirmation and delivery status. It is deployed on Render with a TiDB database.

### Q2. Why did you choose Spring Boot?
Auto-configuration, embedded server, mature security and JPA integration, and it is the most common Java backend stack in job descriptions.

### Q3. Why React?
Component-based UI, fast Vite dev server, easy state management for cart/admin views, and a large ecosystem.

### Q4. Explain the architecture.
Browser → Spring Boot (serves React static files and `/api/**`) → TiDB. Razorpay Checkout is external; the backend creates the order and verifies the signature.

### Q5. How does JWT authentication work?
On login, the server creates a signed token containing the user’s email, role and token version. The client sends it as `Authorization: Bearer <token>`. A filter verifies the signature and expiry, loads the user, compares the token version and sets the security context.

### Q6. Why stateless authentication?
No server session, so the API scales horizontally and works well with mobile/SPA clients.

### Q7. How does logout invalidate a JWT?
JWTs cannot be deleted server-side, so each user has a `token_version`. Logout increments it; the filter rejects tokens whose version is older.

### Q8. Difference between 401 and 403?
401 = not authenticated (no/invalid token). 403 = authenticated but not allowed (e.g., customer calling an admin endpoint).

### Q9. How do you stop a customer viewing another customer’s order?
Queries include the authenticated email (`findByIdAndUserEmail`). If no row matches, return 404 — this also avoids leaking whether the order exists.

### Q10. How are passwords stored?
BCrypt hashes with a per-password salt. The original password cannot be recovered.

### Q11. How do you prevent two customers buying the last item?
Checkout runs in a transaction, locks the product row with `SELECT ... FOR UPDATE`, checks stock, decrements it, and commits. A second transaction waits for the lock and then sees the updated stock.

### Q12. Why native queries for the locks?
Hibernate generated `FOR UPDATE OF alias`; MySQL accepts it, TiDB does not. Native `FOR UPDATE` is portable across both.

### Q13. What is `@Transactional` doing in checkout?
It makes the whole checkout atomic: if any step fails, stock, order, payment and cart changes roll back together.

### Q14. Why DTOs instead of returning entities?
Control over the API shape, no lazy-loading leaks, no accidental exposure of passwords/internal fields, and easier validation.

### Q15. What was the LazyInitializationException?
With `open-in-view=false`, lazy collections cannot be read after the transaction ends. Mapping an order in the controller touched lazy `items`/`payment`. Fixed with `@EntityGraph`.

### Q16. Why is `open-in-view` false?
It avoids hidden database queries during view rendering and makes transaction boundaries explicit. The trade-off is that you must fetch what you need.

### Q17. How does Razorpay integration work?
Server creates a Razorpay order with the amount in paise. Checkout.js opens with that order id. After payment, the handler sends order id, payment id and signature to the backend. The backend recomputes HMAC-SHA256 with the secret and compares.

### Q18. Why verify the signature?
Otherwise anyone could POST a fake “payment successful” request.

### Q19. What happens if the user closes the Razorpay popup?
The payment stays `PENDING`. A production system should use a webhook to reconcile.

### Q20. How does COD work?
Checkout creates a `PENDING` COD payment. An admin collects cash and calls the confirm endpoint, which marks it `SUCCESS`.

### Q21. How does delivery tracking work?
Admin updates `delivery_status`; the customer endpoint returns the current status plus a timeline. The order status is synced to the delivery status.

### Q22. How does the admin dashboard update?
It polls `/api/admin/orders` every 10 seconds. Polling is simple and reliable; WebSockets/SSE would be the next step for real-time.

### Q23. How is CORS configured?
`CORS_ALLOWED_ORIGINS` is a comma-separated list read by Spring Security. In production the frontend is same-origin, so CORS is mostly for local development.

### Q24. How do you validate requests?
Bean Validation annotations on DTOs (`@NotBlank`, `@Email`, `@Size`, `@Min`) and a global handler that returns 400 with field messages.

### Q25. How do you prevent SQL injection?
JPA repository methods and parameter binding; no string-concatenated SQL.

### Q26. What about XSS and CSRF?
React escapes rendered text, which mitigates XSS. The API uses a bearer header rather than cookies, so classic CSRF is not the main risk; the bigger concern is XSS stealing the token from `sessionStorage`. httpOnly cookies + refresh tokens would improve this.

### Q27. How did you test it?
JUnit 5 + MockMvc integration tests against H2, plus a Mockito unit test. They cover auth, authorization, cart, checkout, stock, cancellation, payments, Razorpay signatures, delivery and error codes.

### Q28. Why H2 for tests?
Fast, in-memory, no external dependency; Flyway/Testcontainers would be the production-grade upgrade.

### Q29. How is it deployed?
Docker multi-stage build on Render. The React build is copied into Spring Boot static resources, so one service serves UI + API. TiDB Cloud stores data. Environment variables hold secrets.

### Q30. What would you improve next?
Razorpay webhook, refresh tokens/httpOnly cookies, rate limiting, password reset, Flyway migrations, order pagination, delivery event history, admin customer UI, CI/CD and monitoring.

### Q31. What was the hardest bug?
The TiDB `FOR UPDATE OF` incompatibility, because it only appeared on the live database, not on MySQL/H2. Reading the SQL Hibernate generated and replacing it with native `FOR UPDATE` fixed it.

### Q32. Why not microservices?
The domain is small; a modular monolith is simpler to deploy, test and reason about. Microservices would add network, observability and deployment overhead with no benefit at this scale.

### Q33. How would you scale it?
Add Redis caching for products, paginate order/admin lists, move image hosting to a CDN, use a connection pool tuned for the DB, add read replicas, and move payment reconciliation to webhooks/queues.

### Q34. How are secrets managed?
Locally in a git-ignored `.env`; on Render as environment variables. No secrets are committed to Git.

### Q35. What happens if the database is down?
Requests fail with 500; the exception handler logs the cause. A production system would add retries, health checks and alerting.

---

## 16. Two-minute interview explanation (script)

> “ShopSmart is a full-stack e-commerce application I built with Java 17, Spring Boot, Spring Security, JPA and React.
>
> Customers register and log in using JWT authentication. They can browse products with search, category filtering and price sorting, add items to a cart, and check out with either Cash on Delivery or Razorpay. Checkout is transactional and uses row-level locks so stock cannot go negative. Customers can see their order history, track delivery and cancel before shipment.
>
> Admins have a separate dashboard where they can see live orders, confirm COD payments, update delivery status, and add, edit or deactivate products.
>
> On the security side, passwords are BCrypt-hashed, JWTs are stateless and invalidated on logout using a token version, endpoints are role-protected, and every customer resource is ownership-checked. Razorpay payments are verified with an HMAC signature on the server.
>
> The app is deployed on Render with a TiDB MySQL-compatible database. The React build is served by Spring Boot, so one service handles both UI and API.
>
> The main challenges were fixing lazy-loading issues with JPA, handling a MySQL enum mismatch, and adapting Hibernate’s `FOR UPDATE` SQL for TiDB. I also wrote 16 automated tests covering auth, cart, checkout, payments and authorization.”

---

## 17. If they ask “Did you use AI?”

Be honest and confident:

> “I used AI as a debugging and productivity assistant — for example to trace a lazy-loading error or review SQL. I designed and understand the architecture and can walk through any module: the JWT filter, checkout transaction, Razorpay verification, admin dashboard or database schema.”

Then prove it by explaining one flow in detail (checkout or JWT). Interviewers care that you understand the code, not that you typed every character.

---

## 18. Glossary

- **JWT** — JSON Web Token, a signed stateless token.
- **BCrypt** — adaptive password hashing algorithm.
- **DTO** — Data Transfer Object; API-shaped object.
- **JPA / Hibernate** — ORM that maps Java objects to tables.
- **EntityGraph** — tells JPA which relationships to fetch in one query.
- **Pessimistic lock** — database row lock (`FOR UPDATE`) to prevent concurrent modification.
- **HMAC** — keyed hash used to verify Razorpay signatures.
- **CORS** — browser rule controlling which origins may call an API.
- **Soft delete** — mark a row inactive instead of deleting it.
- **Cold start** — delay when a sleeping free server wakes up.

---

## 19. Current status

| Area | Status |
|---|---|
| Customer storefront | Complete |
| Admin console | Complete for products, orders, COD, delivery |
| JWT auth + roles | Complete |
| Cart + checkout | Complete |
| COD | Complete |
| Razorpay test mode | Complete (order create + signature verify) |
| Delivery tracking | Complete (derived timeline) |
| Tests | 16 passing |
| Deployment | Live on Render + TiDB |
| Database migrations | Not implemented |
| Razorpay webhook/refunds | Not implemented |
| Email/password reset | Not implemented |
| Rate limiting | Not implemented |
| CI/CD | Not implemented |
