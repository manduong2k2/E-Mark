import { useContext, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ProductCard.css';
import defaultProductImage from '../../../assets/product.png';
import { AuthContext } from '../../../contexts/AuthContext';

function ProductCard({ product, onOpenVariantPopup }) {
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const { name, brand, variants } = product;
  const [isFavorite, setIsFavorite] = useState(false);

  // Get first variant image
  const imageUrl = variants && variants.length > 0 && variants[0].images && variants[0].images.length > 0
    ? variants[0].images[0].url
    : defaultProductImage;

  // Get first variant
  const variantCount = variants?.length;

  const firstVariant = variants && variantCount > 0 ? variants[0] : null;

  // Get price range
  const prices = variants?.map(v => v.price) ?? [];
  const minPrice = prices.length ? Math.min(...prices) : 0;
  const maxPrice = prices.length ? Math.max(...prices) : 0;
  const priceDisplay = minPrice === maxPrice ? `$${minPrice.toFixed(2)}` : `$${minPrice.toFixed(2)} ~ $${maxPrice.toFixed(2)}`;

  const handleImageError = (e) => {
    e.target.onerror = null;
    e.target.src = "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80";
  };

  const handleAddToCart = (e) => {
    e.stopPropagation();
    onOpenVariantPopup && onOpenVariantPopup(product);
  };

  const toggleFavorite = (e) => {
    e.stopPropagation();
    setIsFavorite(!isFavorite);
  };

  return (
    <div className="product-card" onClick={() => navigate(`/product/${product.id}`)}>
      {/* Top Badges Row */}
      <div className="product-card-badges">
        <button className="favorite-btn" onClick={toggleFavorite} title="Add to favorites">
          <i className={`${isFavorite ? 'fas' : 'far'} fa-heart`}></i>
        </button>
      </div>

      {/* Image Container */}
      <div className="img-container">
        <img
          src={imageUrl}
          alt={name}
          className="product-image"
          onError={handleImageError}
        />

        {brand && (
          <div className="brand-tag">
            <i className="fas fa-tag"></i>
            <span>{brand.name || 'No Brand'}</span>
          </div>
        )}
      </div>

      {/* Content */}
      <div className="product-content">
        {/* Title & Variant Name */}
        <div className="product-header">
          <div>
            <h3 className="product-title">{name}</h3>
            <p className="variant-count">{variantCount > 1 && `+${variantCount} other versions`}</p>
          </div>
        </div>

        {/* Price & Button Row */}
        <div className="product-footer">
          <span>
            <div className="price-section">
              <span className="price-value">{priceDisplay}</span>
            </div>
            {/* Stock Status */}
            {firstVariant && (
              <div className="stock-info">
                <div className="stock-value">
                  <i className="fas fa-check-circle"></i>
                  <span>{firstVariant.stock} in stock</span>
                </div>
              </div>
            )}
          </span>

          <button className="btn-add-cart" onClick={handleAddToCart} title="Add to cart" aria-label="Add to cart">
            <i className="fas fa-shopping-bag"></i>
          </button>
        </div>
      </div>
    </div>
  );
}

export default ProductCard;
