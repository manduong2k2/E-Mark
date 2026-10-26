import { useNavigate } from 'react-router-dom';
import './ProductCard.css';

const ProductCard = ({ product }) => {
  const navigate = useNavigate();
  
  // Lấy image từ variant đầu tiên
  const imageUrl = product.variants?.[0]?.images?.[0]?.url;
  const price = product.variants?.[0]?.price;

  const handleCardClick = (e) => {
    e.stopPropagation();
    if (product.id) {
      navigate(`/product/${product.id}`);
    }
  };

  const stock = product.variants.reduce((total, variant) => total += variant.stock, 0);

  return (
    <div className="chatbot-product-card" onClick={handleCardClick} style={{ cursor: 'pointer' }}>
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
          <p className="product-card-brand fst-italic mb-0">{product.brand.name}</p>
        )}
        {price && (
          <p className="product-card-price">
            {new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: 'USD',
            }).format(price)}
          </p>
        )}
        {product.status && (
          <p className="product-card-stock mb-0">{
            stock > 0 ? stock + ' IN-STOCK' : 
            'SOLD-OUT'
          }</p>
        )}
      </div>
    </div>
  );
};

export default ProductCard;
