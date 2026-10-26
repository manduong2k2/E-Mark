import React from 'react';
import { useNavigate } from 'react-router-dom';
import './BrandCard.css';

const BrandCard = ({ brand }) => {
  const navigate = useNavigate();

  const handleCardClick = (e) => {
    e.stopPropagation();
    if (brand.id) {
      navigate(`/brand/${brand.id}`);
    }
  };

  return (
    <div className="chatbot-brand-card" onClick={handleCardClick} style={{ cursor: 'pointer' }}>
      <div className="brand-card-logo">
        {brand.image ? (
          <img src={brand.image} alt={brand.name} />
        ) : (
          <div className="placeholder">
            <i className="fa-solid fa-trademark"></i>
          </div>
        )}
      </div>
      <div className="brand-card-info">
        <h4 className="brand-card-name">{brand.name}</h4>
        {brand.description && (
          <p className="brand-card-description">{brand.description}</p>
        )}
        {brand.country && (
          <p className="brand-card-country">
            <i className="fa-solid fa-location-dot"></i>
            {brand.country}
          </p>
        )}
      </div>
    </div>
  );
};

export default BrandCard;
