
import { useEffect, useState } from "react";
import api from "./services/api";
import Auth from "./Auth";
import Orders from "./Orders";
import ProductDetail from "./ProductDetail";
import "./App.css";

function App() {
    const [products, setProducts] = useState([]);
    const [cart, setCart] = useState({ items: [], total: 0 });
    const [loading, setLoading] = useState(true);

    const [paymentMethod, setPaymentMethod] = useState("COD");
    const [order, setOrder] = useState(null);
    const [checkoutLoading, setCheckoutLoading] = useState(false);

    const [cartLoading, setCartLoading] = useState(false);
    const [error, setError] = useState("");
    const [cartMessage, setCartMessage] = useState("");

    const [showAuth, setShowAuth] = useState(false);
    const [showCart, setShowCart] = useState(false);
    const [showOrders, setShowOrders] = useState(false);

    const [searchTerm, setSearchTerm] = useState("");
    const [category, setCategory] = useState("");
    const [categories, setCategories] = useState([]);
    const [selectedProduct, setSelectedProduct] = useState(null);

    const [user, setUser] = useState(() => {
        try {
            return JSON.parse(sessionStorage.getItem("shopsmart_user"));
        } catch {
            return null;
        }
    });

    // Authentication configuration
    function getAuthConfig() {
        const token = sessionStorage.getItem("shopsmart_token");

        return {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        };
    }

    // Load product categories
    useEffect(() => {
        api.get("/api/products/categories")
            .then((response) => setCategories(response.data))
            .catch((err) => console.error("Category loading error:", err));
    }, []);

    // Load products (debounced search + category filter)
    useEffect(() => {
        const timer = setTimeout(() => {
            setLoading(true);
            setError("");

            const params = {
                page: 0,
                size: 20,
                sortBy: "name",
                direction: "asc",
            };

            if (searchTerm.trim()) params.search = searchTerm.trim();
            if (category) params.category = category;

            api.get("/api/products", { params })
                .then((response) => {
                    setProducts(response.data.content || []);
                })
                .catch((err) => {
                    console.error("Product loading error:", err);
                    setError("Unable to load products. Please check your backend.");
                })
                .finally(() => setLoading(false));
        }, 250);

        return () => clearTimeout(timer);
    }, [searchTerm, category]);

    // Load cart from backend
    async function loadCart() {
        if (!sessionStorage.getItem("shopsmart_token")) {
            setShowAuth(true);
            return;
        }

        setCartLoading(true);

        try {
            const response = await api.get("/api/cart", getAuthConfig());
            setCart(response.data);
        } catch (err) {
            console.error("Cart loading error:", err);
            setCartMessage(
                err.response?.data?.message || "Unable to load your cart."
            );
        } finally {
            setCartLoading(false);
        }
    }

    // Handle login and registration
    function handleAuth(authData) {
        const loggedInUser = {
            email: authData.email,
            fullName: authData.fullName,
            role: authData.role,
        };

        setUser(loggedInUser);
        setShowAuth(false);
        setCartMessage("");

        sessionStorage.setItem(
            "shopsmart_user",
            JSON.stringify(loggedInUser)
        );
    }

    // Logout
    async function handleLogout() {
        try {
            await api.post(
                "/api/auth/logout",
                {},
                getAuthConfig()
            );
        } catch (err) {
            console.error("Logout request failed:", err);
        } finally {
            sessionStorage.removeItem("shopsmart_token");
            sessionStorage.removeItem("shopsmart_user");

            setUser(null);
            setCart({ items: [], total: 0 });
            setOrder(null);
            setShowCart(false);
            setShowOrders(false);
            setShowAuth(false);
            setCartMessage("");
        }
    }

    // Add product to cart
    async function handleAddToCart(productId) {
        if (!user || !sessionStorage.getItem("shopsmart_token")) {
            setShowAuth(true);
            return;
        }

        setCartMessage("");

        try {
            await api.post(
                "/api/cart/items",
                {
                    productId: productId,
                    quantity: 1,
                },
                getAuthConfig()
            );

            setCartMessage("Product successfully added to your cart!");

            if (showCart) {
                await loadCart();
            }
        } catch (err) {
            console.error("Add to cart error:", err);
            setCartMessage(
                err.response?.data?.message ||
                "Unable to add product to cart. Please try again."
            );
        }
    }

    // Update product quantity
    async function handleQuantityChange(productId, newQuantity) {
        if (newQuantity < 1) return;

        try {
            const response = await api.put(
                `/api/cart/items/${productId}`,
                { quantity: newQuantity },
                getAuthConfig()
            );

            setCart(response.data);
            setCartMessage("");
        } catch (err) {
            console.error("Quantity update error:", err);
            setCartMessage(
                err.response?.data?.message ||
                "Unable to update quantity."
            );
        }
    }

    // Remove product from cart
    async function handleRemoveItem(productId) {
        try {
            const response = await api.delete(
                `/api/cart/items/${productId}`,
                getAuthConfig()
            );

            setCart(response.data);
            setCartMessage("Product removed from cart.");
        } catch (err) {
            console.error("Remove item error:", err);
            setCartMessage(
                err.response?.data?.message ||
                "Unable to remove product."
            );
        }
    }

    // Open cart
    async function handleOpenCart() {
        if (!user || !sessionStorage.getItem("shopsmart_token")) {
            setShowAuth(true);
            return;
        }

        setOrder(null);
        setShowAuth(false);
        setShowCart(true);
        setShowOrders(false);
        setCartMessage("");

        await loadCart();
    }

    // Open order history
    async function handleOpenOrders() {
        if (!user || !sessionStorage.getItem("shopsmart_token")) {
            setShowAuth(true);
            return;
        }

        setOrder(null);
        setShowAuth(false);
        setShowCart(false);
        setCartMessage("");
        setShowOrders(true);
    }

    // Checkout
    async function handleCheckout() {
        if (cart.items.length === 0) {
            setCartMessage("Your cart is empty.");
            return;
        }

        setCheckoutLoading(true);
        setCartMessage("");

        try {
            const response = await api.post(
                "/api/orders",
                {
                    paymentMethod: paymentMethod,
                },
                getAuthConfig()
            );

            // Save the successful order response
            setOrder(response.data);

            // Clear the frontend cart
            setCart({ items: [], total: 0 });
            setShowCart(false);
            setCartMessage("");
        } catch (err) {
            console.error("Checkout error:", err);

            setCartMessage(
                err.response?.data?.message ||
                "Checkout failed. Please try again."
            );
        } finally {
            setCheckoutLoading(false);
        }
    }

    // Continue shopping after successful order
    function handleContinueShopping() {
        setOrder(null);
        setShowCart(false);
        setShowOrders(false);
        setCartMessage("");
    }

    // Login/register page
    if (showAuth) {
        return (
            <div>
                <button
                    className="back-button"
                    onClick={() => setShowAuth(false)}
                >
                    ← Back to ShopSmart
                </button>

                <Auth onAuth={handleAuth} />
            </div>
        );
    }

    return (
        <div className="app">

            {/* Navigation */}
            <header className="navbar">
                <h1
                    onClick={handleContinueShopping}
                    style={{ cursor: "pointer" }}
                >
                    ShopSmart
                </h1>

                <nav>
                    <a
                        href="#home"
                        onClick={handleContinueShopping}
                    >
                        Home
                    </a>

                    <a
                        href="#products"
                        onClick={handleContinueShopping}
                    >
                        Products
                    </a>

                    <a href="#about">About</a>
                </nav>

                <div className="nav-actions">
                    {user ? (
                        <>
                            <span className="welcome-user">
                                Hi, {user.fullName}
                            </span>

                            <button
                                className="cart-button"
                                onClick={handleOpenCart}
                            >
                                🛒 My Cart ({cart.items.length})
                            </button>

                            <button
                                className="cart-button"
                                onClick={handleOpenOrders}
                            >
                                📦 My Orders
                            </button>

                            <button
                                className="cart-button"
                                onClick={handleLogout}
                            >
                                Logout
                            </button>
                        </>
                    ) : (
                        <button
                            className="cart-button"
                            onClick={() => setShowAuth(true)}
                        >
                            Login / Register
                        </button>
                    )}
                </div>
            </header>

            {/* Order confirmation page */}
            {order ? (
                <main className="order-success">
                    <div className="success-icon">✓</div>

                    <h2>Order Placed Successfully!</h2>

                    <p>Thank you for shopping with ShopSmart.</p>

                    <div className="order-details">
                        <p>
                            <strong>Order ID:</strong>{" "}
                            {order.id ?? order.orderId ?? "N/A"}
                        </p>

                        <p>
                            <strong>Order Status:</strong>{" "}
                            {order.status ?? "PLACED"}
                        </p>

                        <p>
                            <strong>Payment Method:</strong>{" "}
                            {order.paymentMethod ?? paymentMethod}
                        </p>

                        <p>
                            <strong>Total Amount:</strong> ₹
                            {Number(order.total ?? 0).toLocaleString("en-IN")}
                        </p>
                    </div>

                    <p className="delivery-message">
                        Your order is being processed. You can expect
                        delivery updates soon.
                    </p>

                    <button
                        className="checkout-button"
                        onClick={handleContinueShopping}
                    >
                        Continue Shopping
                    </button>
                </main>

            ) : showOrders ? (
                <Orders
                    onBack={() => {
                        setShowOrders(false);
                        setCartMessage("");
                    }}
                />

            ) : showCart ? (

                /* Cart page */
                <main className="cart-section">
                    <button
                        className="back-button"
                        onClick={() => {
                            setShowCart(false);
                            setCartMessage("");
                        }}
                    >
                        ← Continue Shopping
                    </button>

                    <h2>My Shopping Cart</h2>

                    {cartMessage && (
                        <p className="cart-message">{cartMessage}</p>
                    )}

                    {cartLoading ? (
                        <p>Loading your cart...</p>
                    ) : cart.items.length === 0 ? (
                        <div className="empty-cart">
                            <h3>Your cart is empty</h3>
                            <p>Add some products to start shopping!</p>
                        </div>
                    ) : (
                        <>
                            <div className="cart-list">
                                {cart.items.map((item) => (
                                    <article
                                        className="cart-item"
                                        key={item.productId}
                                    >
                                        <div className="cart-item-info">
                                            <h3>{item.name}</h3>

                                            <p>
                                                Price: ₹
                                                {Number(item.unitPrice).toLocaleString("en-IN")}
                                            </p>

                                            <p>
                                                Subtotal: ₹
                                                {Number(item.lineTotal).toLocaleString("en-IN")}
                                            </p>
                                        </div>

                                        <div className="quantity-controls">
                                            <button
                                                onClick={() =>
                                                    handleQuantityChange(
                                                        item.productId,
                                                        item.quantity - 1
                                                    )
                                                }
                                                disabled={item.quantity <= 1}
                                            >
                                                −
                                            </button>

                                            <span>{item.quantity}</span>

                                            <button
                                                onClick={() =>
                                                    handleQuantityChange(
                                                        item.productId,
                                                        item.quantity + 1
                                                    )
                                                }
                                            >
                                                +
                                            </button>
                                        </div>

                                        <button
                                            className="remove-button"
                                            onClick={() =>
                                                handleRemoveItem(item.productId)
                                            }
                                        >
                                            Remove
                                        </button>
                                    </article>
                                ))}
                            </div>

                            <div className="cart-total">
                                <h3>
                                    Total Amount: ₹
                                    {Number(cart.total).toLocaleString("en-IN")}
                                </h3>

                                <div className="payment-selection">
                                    <h3>Select Payment Method</h3>

                                    <label>
                                        <input
                                            type="radio"
                                            name="payment"
                                            value="COD"
                                            checked={paymentMethod === "COD"}
                                            onChange={(e) =>
                                                setPaymentMethod(e.target.value)
                                            }
                                        />
                                        Cash on Delivery (COD)
                                    </label>

                                    <p className="payment-note">
                                        Pay when your order is delivered.
                                    </p>

                                    <button
                                        className="checkout-button"
                                        onClick={handleCheckout}
                                        disabled={
                                            checkoutLoading ||
                                            cart.items.length === 0
                                        }
                                    >
                                        {checkoutLoading
                                            ? "Processing..."
                                            : "Proceed to Checkout"}
                                    </button>
                                </div>
                            </div>
                        </>
                    )}
                </main>

            ) : (

                /* Home page */
                <>
                    <section className="hero" id="home">
                        <p className="hero-label">
                            WELCOME TO SHOPSMART
                        </p>

                        <h2>Discover Products You'll Love</h2>

                        <p>
                            Quality products. Simple shopping. Great experiences.
                        </p>

                        <a href="#products" className="shop-button">
                            Shop Now
                        </a>
                    </section>

                    {/* Products */}
                    <main id="products" className="products-section">
                        <h2>Our Products</h2>

                        <p className="section-description">
                            Explore our latest collection.
                        </p>

                        <div className="catalog-controls">
                            <input
                                type="search"
                                placeholder="Search products..."
                                value={searchTerm}
                                onChange={(event) =>
                                    setSearchTerm(event.target.value)
                                }
                            />

                            <select
                                value={category}
                                onChange={(event) =>
                                    setCategory(event.target.value)
                                }
                            >
                                <option value="">All categories</option>
                                {categories.map((name) => (
                                    <option key={name} value={name}>
                                        {name}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {cartMessage && (
                            <p className="cart-message">{cartMessage}</p>
                        )}

                        {loading && <p>Loading products...</p>}

                        {error && <p className="error">{error}</p>}

                        <div className="product-grid">
                            {products.map((product) => (
                                <article
                                    className="product-card"
                                    key={product.id}
                                    onClick={() => setSelectedProduct(product)}
                                >
                                    <img
                                        src={product.imageUrl}
                                        alt={product.name}
                                        onError={(e) => {
                                            e.currentTarget.src =
                                                "https://placehold.co/400x300?text=ShopSmart";
                                        }}
                                    />

                                    <div className="product-info">
                                        <p className="category">
                                            {product.category}
                                        </p>

                                        <h3>{product.name}</h3>

                                        <p className="price">
                                            ₹
                                            {Number(product.price).toLocaleString("en-IN")}
                                        </p>

                                        <button
                                            className="add-button"
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                handleAddToCart(product.id);
                                            }}
                                        >
                                            Add to Cart
                                        </button>
                                    </div>
                                </article>
                            ))}
                        </div>

                        {!loading && !error && products.length === 0 && (
                            <p>No products available.</p>
                        )}
                    </main>
                </>
            )}

            <ProductDetail
                product={selectedProduct}
                onClose={() => setSelectedProduct(null)}
                onAdd={(id) => {
                    handleAddToCart(id);
                    setSelectedProduct(null);
                }}
            />

            {/* Footer */}
            <footer id="about">
                <h2>ShopSmart</h2>
                <p>Your shopping experience, made simple.</p>
            </footer>
        </div>
    );
}

export default App;
