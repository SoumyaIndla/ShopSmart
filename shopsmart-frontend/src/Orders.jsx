import { useEffect, useState } from "react";
import api from "./services/api";
import "./Orders.css";

function Orders({ onBack }) {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [tracking, setTracking] = useState(null);

    const token = sessionStorage.getItem("shopsmart_token");
    const config = {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    };

    async function loadOrders() {
        try {
            const response = await api.get("/api/orders", config);
            setOrders(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load your orders."
            );
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        let active = true;

        api.get("/api/orders", config)
            .then((response) => {
                if (active) setOrders(response.data);
            })
            .catch((err) => {
                if (active) {
                    setError(
                        err.response?.data?.message ||
                        "Unable to load your orders."
                    );
                }
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    async function handleCancel(orderId) {
        if (!window.confirm("Cancel this order?")) return;

        setError("");

        try {
            await api.post(`/api/orders/${orderId}/cancel`, {}, config);
            setTracking(null);
            await loadOrders();
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to cancel this order."
            );
        }
    }

    function canCancel(order) {
        if (order.status === "CANCELLED") return false;

        return ![
            "SHIPPED",
            "OUT_FOR_DELIVERY",
            "DELIVERED",
            "RETURNED",
        ].includes(order.deliveryStatus);
    }

    async function handleTrack(orderId) {
        setError("");

        try {
            const response = await api.get(
                `/api/orders/${orderId}/tracking`,
                config
            );

            setTracking(response.data);
        } catch (err) {
            setError(
                err.response?.data?.message ||
                "Unable to load delivery tracking."
            );
        }
    }

    function formatDate(value) {
        if (!value) return "—";
        return new Date(value).toLocaleString("en-IN", {
            dateStyle: "medium",
            timeStyle: "short",
        });
    }

    return (
        <main className="orders-section">
            <button className="back-button" onClick={onBack}>
                ← Continue Shopping
            </button>

            <h2>My Orders</h2>
            <p className="section-description">
                Track payments and delivery for every order you have placed.
            </p>

            {error && <p className="cart-message">{error}</p>}

            {loading ? (
                <p>Loading your orders...</p>
            ) : orders.length === 0 ? (
                <div className="empty-cart">
                    <h3>No orders yet</h3>
                    <p>Place your first order to see it here.</p>
                </div>
            ) : (
                <div className="orders-list">
                    {orders.map((order) => (
                        <article className="order-card" key={order.id}>
                            <div className="order-card-head">
                                <h3>Order #{order.id}</h3>

                                <span
                                    className={
                                        "status-badge " +
                                        (order.status === "CANCELLED"
                                            ? "cancelled"
                                            : "")
                                    }
                                >
                                    {order.status}
                                </span>
                            </div>

                            <p className="order-meta">
                                Placed: {formatDate(order.createdAt)}
                            </p>

                            <dl className="order-facts">
                                <div>
                                    <dt>Delivery</dt>
                                    <dd>{order.deliveryStatus}</dd>
                                </div>
                                <div>
                                    <dt>Payment</dt>
                                    <dd>
                                        {order.paymentMethod ?? "—"}
                                        {order.paymentStatus
                                            ? ` · ${order.paymentStatus}`
                                            : ""}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Items</dt>
                                    <dd>{order.items?.length ?? 0}</dd>
                                </div>
                                <div>
                                    <dt>Total</dt>
                                    <dd>
                                        ₹
                                        {Number(order.total ?? 0).toLocaleString(
                                            "en-IN"
                                        )}
                                    </dd>
                                </div>
                            </dl>

                            <div className="order-actions">
                                <button
                                    className="checkout-button"
                                    onClick={() => handleTrack(order.id)}
                                >
                                    Track Order
                                </button>

                                {canCancel(order) && (
                                    <button
                                        className="remove-button"
                                        onClick={() => handleCancel(order.id)}
                                    >
                                        Cancel Order
                                    </button>
                                )}
                            </div>
                        </article>
                    ))}
                </div>
            )}

            {tracking && (
                <section className="tracking-panel">
                    <div className="order-card-head">
                        <h3>Tracking · Order #{tracking.orderId}</h3>

                        <button
                            className="remove-button"
                            onClick={() => setTracking(null)}
                        >
                            Close
                        </button>
                    </div>

                    <p className="order-meta">
                        Current delivery status:{" "}
                        <strong>{tracking.deliveryStatus}</strong>
                    </p>

                    <ol className="timeline">
                        {tracking.timeline.map((step) => (
                            <li
                                key={step.status}
                                className={
                                    "timeline-step" +
                                    (step.completed ? " done" : "") +
                                    (step.current ? " current" : "")
                                }
                            >
                                <span className="timeline-dot" />
                                <span>
                                    {step.status.replace(/_/g, " ")}
                                </span>
                            </li>
                        ))}
                    </ol>
                </section>
            )}
        </main>
    );
}

export default Orders;
