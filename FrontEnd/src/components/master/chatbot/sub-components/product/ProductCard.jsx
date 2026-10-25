import './ProductCard.css';

const ProductCard = ({ product }) => {
  // Lấy image từ variant đầu tiên
  const imageUrl = product.variants?.[0]?.images?.[0]?.url;
  const price = product.variants?.[0]?.price;

  return (
    <div className="chatbot-product-card">
      <div className="product-card-image">
        {imageUrl ? (
          <img src={imageUrl} alt={product.name} />
        ) : (
          <div className="placeholder">
            <i className="fa-solid fa-cube"></i>
          </div>
        )}
      </div>
      <div className="product-card-info">
        <h4 className="product-card-name">{product.name}</h4>
        {product.brand?.name && (
          <p className="product-card-brand mb-0">{product.brand.name}</p>
        )}
        {price && (
          <p className="product-card-price">
            {new Intl.NumberFormat('vi-VN', {
              style: 'currency',
              currency: 'VND',
            }).format(price)}
          </p>
        )}
        {product.status && (
          <p className="product-card-status mb-0">{product.status}</p>
        )}
      </div>
    </div>
  );
};

export default ProductCard;
