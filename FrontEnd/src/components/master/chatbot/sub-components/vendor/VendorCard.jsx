import React from 'react';
import './VendorCard.css';

const VendorCard = ({ vendor }) => {
  return (
    <div className="chatbot-vendor-card">
      <div className="vendor-card-avatar">
        {vendor.logo ? (
          <img src={vendor.logo} alt={vendor.name} />
        ) : (
          <div className="placeholder">
            <i className="fa-solid fa-store"></i>
          </div>
        )}
      </div>
      <div className="vendor-card-info">
        <h4 className="vendor-card-name">{vendor.name}</h4>
        {vendor.description && (
          <p className="vendor-card-description">{vendor.description}</p>
        )}
        {vendor.rating && (
          <div className="vendor-card-stats">
            <span className="vendor-rating">
              <i className="fa-solid fa-star"></i> {vendor.rating}
            </span>
          </div>
        )}
      </div>
    </div>
  );
};

export default VendorCard;
