import { useCallback, useEffect, useState } from "react";
import api from "./services/api";
import "./Admin.css";

const DELIVERY_STATUSES = [
    "PROCESSING",
    "CONFIRMED",
    "PACKED",
    "SHIPPED",
    "OUT_FOR_DELIVERY",
    "DELIVERED",
    "RETURNED",
];

const EMPTY_PRODUCT = {
    name: "",
    description: "",
    price: "",
    stock: "",
    category: "",
    imageUrl: "",
};

function Admin() {
    const [token, setToken] = useState(() =>
        sessionStorage.getItem("shopsmart_admin_token")
    );
    const [email, setEmail] = useState("admin@shopsmart.com");
    const [password, setPassword] = useState("");
    const [loginError, setLoginError] = useState("");

    const [tab, setTab] = useState("orders");
    const [orders, setOrders] = useState([]);
    const [products, setProducts] = useState([]);
    const [form, setForm] = useState(EMPTY_PRODUCT);
    const [editingId, setEditingId] = useState(null);
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const [lastUpdated, setLastUpdated] = useState("");

    const auth = useCallback(
        () => ({
            headers: { Authorization: `Bearer ${token}` },
        }),
        [token]
    );

    const logout = useCallback(() => {
        sessionStorage.removeItem("shopsmart_admin_token");
        setToken(null);
        setOrders([]);
        setProducts([]);
    }, []);

    async function handleLogin(event) {
        event.preventDefault();
        setLoginError("");

        try {
            const { data } = await api.post("/api/auth/login", {
                email,
                password,
            });

            if (data.role !== "ADMIN") {
                setLoginError("This account is not an administrator.");
                return;
            }

            sessionStorage.setItem("shopsmart_admin_token", data.token);
            setToken(data.token);
            setPassword("");
        } catch (err) {
            setLoginError(
                err.response?.data?.message || "Unable to sign in."
            );
        }
    }

    const loadOrders = useCallback(async () => {
        if (!token) return;

        try {
            const { data } = await api.get("/api/admin/orders", auth());
            setOrders(data);
            setLastUpdated(new Date().toLocaleTimeString("en-IN"));
            setError("");
        } catch (err) {
            if (err.response?.status === 401) {
                logout();
            } else {
                setError(
                    err.response?.data?.message ||
                    "Unable to load orders."
                );
            }
        }
    }, [token, auth, logout]);

    const loadProducts = useCallback(async () => {
        if (!token) return;

        try {
            const { data } = await api.get(
                "/api/admin/products?page=0&size=100",
                auth()
            );
            setProducts(data.content || []);
        } catch (err) {
            if (err.response?.status === 401) logout();
        }
    }, [token, auth, logout]);

    useEffect(() => {
        if (!token) return;

        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadOrders();
        loadProducts();

        // Live dashboard: new/cancelled orders appear within 10 seconds.
        const timer = setInterval(loadOrders, 10000);

        return () => clearInterval(timer);
    }, [token, loadOrders, loadProducts]);

    async function updateDelivery(orderId, deliveryStatus) {
        setBusy(true);
        setError("");

        try {
            await api.patch(
                `/api/admin/orders/${orderId}/delivery-status`,
                { deliveryStatus },
                auth()
            );
            await loadOrders();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to update delivery status."
            );
        } finally {
            setBusy(false);
        }
    }

    async function confirmCod(orderId) {
        setBusy(true);
        setError("");

        try {
            await api.post(
                `/api/payments/cod/${orderId}/confirm`,
                {},
                auth()
            );
            await loadOrders();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to confirm COD payment."
            );
        } finally {
            setBusy(false);
        }
    }

    function startEdit(product) {
        setEditingId(product.id);
        setForm({
            name: product.name || "",
            description: product.description || "",
            price: String(product.price ?? ""),
            stock: String(product.stock ?? ""),
            category: product.category || "",
            imageUrl: product.imageUrl || "",
        });
        setMessage("");
        setError("");
        setTab("add");
    }

    function resetForm() {
        setEditingId(null);
        setForm(EMPTY_PRODUCT);
    }

    async function saveProduct(event) {
        event.preventDefault();
        setBusy(true);
        setMessage("");
        setError("");

        const payload = {
            name: form.name,
            description: form.description,
            price: Number(form.price),
            stock: Number(form.stock),
            category: form.category,
            imageUrl: form.imageUrl,
        };

        try {
            if (editingId) {
                await api.put(
                    `/api/products/${editingId}`,
                    payload,
                    auth()
                );
                setMessage("Product updated successfully.");
            } else {
                await api.post("/api/products", payload, auth());
                setMessage("Product added successfully.");
            }

            resetForm();
            await loadProducts();
            setTab("products");
        } catch (err) {
            setError(
                err.response?.data?.message ||
                (editingId
                    ? "Unable to update the product."
                    : "Unable to add the product.")
            );
        } finally {
            setBusy(false);
        }
    }

    async function deactivateProduct(productId) {
        if (!window.confirm("Deactivate this product?")) return;

        setBusy(true);

        try {
            await api.delete(`/api/products/${productId}`, auth());
            await loadProducts();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to deactivate the product."
            );
        } finally {
            setBusy(false);
        }
    }

    if (!token) {
        return (
            <div className="admin-login">
                <form className="admin-login-card" onSubmit={handleLogin}>
                    <div className="admin-logo">S</div>
                    <h1>ShopSmart Admin</h1>
                    <p className="admin-login-sub">
                        Sign in to manage orders, payments and products.
                    </p>

                    <label>
                        Email
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="username"
                            placeholder="admin@shopsmart.com"
                            required
                        />
                    </label>

                    <label>
                        Password
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            autoComplete="current-password"
                            placeholder="Enter your password"
                            required
                        />
                    </label>

                    {loginError && (
                        <p className="admin-error">{loginError}</p>
                    )}

                    <button className="admin-primary" type="submit">
                        Sign in
                    </button>

                    <a className="admin-link" href="/">
                        ← Back to storefront
                    </a>
                </form>
            </div>
        );
    }

    const pendingPayments = orders.filter(
        (o) => o.paymentStatus === "PENDING" && o.status !== "CANCELLED"
    ).length;
    const pendingShipments = orders.filter(
        (o) =>
            o.status !== "CANCELLED" &&
            !["DELIVERED", "RETURNED"].includes(o.deliveryStatus)
    ).length;

    return (
        <div className="admin-shell">
            <header className="admin-topbar">
                <div>
                    <strong>ShopSmart</strong>
                    <span>Admin Console</span>
                </div>

                <div className="admin-top-actions">
                    <a href="/" className="admin-link">
                        View store
                    </a>
                    <button className="admin-ghost" onClick={logout}>
                        Sign out
                    </button>
                </div>
            </header>

            <nav className="admin-tabs">
                {[
                    ["orders", "Orders"],
                    ["products", "Products"],
                    ["add", "Add / Edit Product"],
                ].map(([key, label]) => (
                    <button
                        key={key}
                        className={tab === key ? "active" : ""}
                        onClick={() => setTab(key)}
                    >
                        {label}
                    </button>
                ))}
            </nav>

            <main className="admin-content">
                {error && <p className="admin-error">{error}</p>}
                {message && <p className="admin-success">{message}</p>}

                <section className="admin-stats">
                    <div>
                        <span>Total orders</span>
                        <strong>{orders.length}</strong>
                    </div>
                    <div>
                        <span>Pending payments</span>
                        <strong>{pendingPayments}</strong>
                    </div>
                    <div>
                        <span>Pending shipments</span>
                        <strong>{pendingShipments}</strong>
                    </div>
                </section>

                {tab === "orders" && (
                    <section>
                        <div className="admin-section-head">
                            <h2>Orders</h2>
                            <div className="admin-head-actions">
                                <span className="admin-live">
                                    ● Live
                                    {lastUpdated
                                        ? ` · updated ${lastUpdated}`
                                        : ""}
                                </span>
                                <button
                                    className="admin-ghost"
                                    onClick={loadOrders}
                                >
                                    Refresh
                                </button>
                            </div>
                        </div>

                        {orders.length === 0 ? (
                            <p>No orders yet.</p>
                        ) : (
                            <div className="admin-table-wrap">
                                <table className="admin-table">
                                    <thead>
                                        <tr>
                                            <th>#</th>
                                            <th>Customer</th>
                                            <th>Total</th>
                                            <th>Payment</th>
                                            <th>Order status</th>
                                            <th>Delivery (update)</th>
                                            <th>Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {orders.map((order) => (
                                            <tr key={order.id}>
                                                <td>{order.id}</td>
                                                <td>
                                                    <strong>
                                                        {order.customerName}
                                                    </strong>
                                                    <br />
                                                    <small>
                                                        {order.customerEmail}
                                                    </small>
                                                </td>
                                                <td>
                                                    ₹
                                                    {Number(
                                                        order.total
                                                    ).toLocaleString(
                                                        "en-IN"
                                                    )}
                                                </td>
                                                <td>
                                                    <div>
                                                        {order.paymentMethod ??
                                                            "—"}
                                                    </div>
                                                    {order.paymentStatus && (
                                                        <span
                                                            className={
                                                                "admin-badge " +
                                                                (order.paymentStatus ===
                                                                "SUCCESS"
                                                                    ? "good"
                                                                    : order.paymentStatus ===
                                                                        "FAILED"
                                                                      ? "bad"
                                                                      : "warn")
                                                            }
                                                        >
                                                            {
                                                                order.paymentStatus
                                                            }
                                                        </span>
                                                    )}
                                                </td>
                                                <td>
                                                    <span
                                                        className={
                                                            "admin-badge " +
                                                            (order.status ===
                                                            "CANCELLED"
                                                                ? "bad"
                                                                : "")
                                                        }
                                                    >
                                                        {order.status}
                                                    </span>
                                                </td>
                                                <td>
                                                    <select
                                                        className="admin-delivery-select"
                                                        value={
                                                            order.deliveryStatus
                                                        }
                                                        disabled={busy}
                                                        title="Change delivery status"
                                                        onChange={(e) =>
                                                            updateDelivery(
                                                                order.id,
                                                                e.target.value
                                                            )
                                                        }
                                                    >
                                                        {DELIVERY_STATUSES.map(
                                                            (status) => (
                                                                <option
                                                                    key={
                                                                        status
                                                                    }
                                                                    value={
                                                                        status
                                                                    }
                                                                >
                                                                    {status}
                                                                </option>
                                                            )
                                                        )}
                                                    </select>
                                                </td>
                                                <td>
                                                    {order.paymentMethod ===
                                                        "COD" &&
                                                        order.paymentStatus ===
                                                            "PENDING" &&
                                                        order.status !==
                                                            "CANCELLED" && (
                                                            <button
                                                                className="admin-primary small"
                                                                disabled={busy}
                                                                onClick={() =>
                                                                    confirmCod(
                                                                        order.id
                                                                    )
                                                                }
                                                            >
                                                                Confirm COD
                                                            </button>
                                                        )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                )}

                {tab === "products" && (
                    <section>
                        <div className="admin-section-head">
                            <h2>Products</h2>
                            <button
                                className="admin-primary small"
                                onClick={() => {
                                    resetForm();
                                    setTab("add");
                                }}
                            >
                                + Add product
                            </button>
                        </div>

                        <div className="admin-product-grid">
                            {products.map((product) => (
                                <article
                                    className="admin-product"
                                    key={product.id}
                                >
                                    <img
                                        src={
                                            product.imageUrl ||
                                            "/placeholder.svg"
                                        }
                                        alt={product.name}
                                        loading="lazy"
                                        decoding="async"
                                        onError={(e) => {
                                            e.currentTarget.src =
                                                "/placeholder.svg";
                                        }}
                                    />

                                    <div>
                                        <h3>{product.name}</h3>
                                        <p>{product.category}</p>
                                        <p>
                                            ₹
                                            {Number(
                                                product.price
                                            ).toLocaleString("en-IN")}
                                            {" · "}
                                            {product.stock} in stock
                                        </p>
                                        <p>
                                            {product.active
                                                ? "Active"
                                                : "Deactivated"}
                                        </p>

                                        <div className="admin-row-actions">
                                            <button
                                                className="admin-ghost small"
                                                onClick={() =>
                                                    startEdit(product)
                                                }
                                            >
                                                Edit
                                            </button>

                                            {product.active && (
                                                <button
                                                    className="admin-danger small"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        deactivateProduct(
                                                            product.id
                                                        )
                                                    }
                                                >
                                                    Deactivate
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </article>
                            ))}
                        </div>
                    </section>
                )}

                {tab === "add" && (
                    <section>
                        <h2>{editingId ? "Edit Product" : "Add Product"}</h2>

                        <form className="admin-form" onSubmit={saveProduct}>
                            <label>
                                Name
                                <input
                                    value={form.name}
                                    onChange={(e) =>
                                        setForm({
                                            ...form,
                                            name: e.target.value,
                                        })
                                    }
                                    required
                                />
                            </label>

                            <label>
                                Description
                                <textarea
                                    rows="3"
                                    value={form.description}
                                    onChange={(e) =>
                                        setForm({
                                            ...form,
                                            description: e.target.value,
                                        })
                                    }
                                    required
                                />
                            </label>

                            <div className="admin-form-row">
                                <label>
                                    Price (₹)
                                    <input
                                        type="number"
                                        min="0.01"
                                        step="0.01"
                                        value={form.price}
                                        onChange={(e) =>
                                            setForm({
                                                ...form,
                                                price: e.target.value,
                                            })
                                        }
                                        required
                                    />
                                </label>

                                <label>
                                    Stock
                                    <input
                                        type="number"
                                        min="0"
                                        value={form.stock}
                                        onChange={(e) =>
                                            setForm({
                                                ...form,
                                                stock: e.target.value,
                                            })
                                        }
                                        required
                                    />
                                </label>

                                <label>
                                    Category
                                    <input
                                        value={form.category}
                                        onChange={(e) =>
                                            setForm({
                                                ...form,
                                                category: e.target.value,
                                            })
                                        }
                                        required
                                    />
                                </label>
                            </div>

                            <label>
                                Image URL
                                <input
                                    placeholder="https://..."
                                    value={form.imageUrl}
                                    onChange={(e) =>
                                        setForm({
                                            ...form,
                                            imageUrl: e.target.value,
                                        })
                                    }
                                />
                            </label>

                            {form.imageUrl && (
                                <img
                                    className="admin-preview"
                                    src={form.imageUrl}
                                    alt="Preview"
                                    onError={(e) => {
                                        e.currentTarget.src =
                                            "/placeholder.svg";
                                    }}
                                />
                            )}

                            <div className="admin-form-actions">
                                <button
                                    className="admin-primary"
                                    type="submit"
                                    disabled={busy}
                                >
                                    {busy
                                        ? "Saving..."
                                        : editingId
                                          ? "Update product"
                                          : "Save product"}
                                </button>

                                {editingId && (
                                    <button
                                        type="button"
                                        className="admin-ghost"
                                        onClick={() => {
                                            resetForm();
                                            setTab("products");
                                        }}
                                    >
                                        Cancel
                                    </button>
                                )}
                            </div>
                        </form>
                    </section>
                )}
            </main>
        </div>
    );
}

export default Admin;
