import "./ProductDetail.css";

function ProductDetail({ product, onClose, onAdd }) {
    if (!product) return null;

    const inStock = (product.stock ?? 0) > 0;

    return (
        <div className="detail-overlay" onClick={onClose}>
            <div
                className="detail-card"
                onClick={(event) => event.stopPropagation()}
            >
                <button className="detail-close" onClick={onClose}>
                    ✕
                </button>

                <img
                    className="detail-image"
                    src={product.imageUrl || "/placeholder.svg"}
                    alt={product.name}
                    onError={(event) => {
                        event.currentTarget.src = "/placeholder.svg";
                    }}
                />

                <div className="detail-body">
                    <p className="category">{product.category}</p>
                    <h2>{product.name}</h2>
                    <p className="detail-description">
                        {product.description}
                    </p>

                    <p className="detail-price">
                        ₹{Number(product.price ?? 0).toLocaleString("en-IN")}
                    </p>

                    <p className={inStock ? "stock in" : "stock out"}>
                        {inStock
                            ? `${product.stock} in stock`
                            : "Out of stock"}
                    </p>

                    <button
                        className="add-button"
                        disabled={!inStock}
                        onClick={() => onAdd(product.id)}
                    >
                        {inStock ? "Add to Cart" : "Unavailable"}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ProductDetail;
