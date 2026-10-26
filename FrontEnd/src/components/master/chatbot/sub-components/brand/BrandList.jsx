import React from 'react';
import BrandCard from './BrandCard';
import './BrandList.css';

const BrandList = ({ data = {} }) => {
  const { data: brands = [], currentPage = 0, totalPages = 1 } = data;

  if (!brands || brands.length === 0) {
    return (
      <div className="chatbot-brand-list">
        <div className="chatbot-empty-state">
          <i className="fa-solid fa-trademark"></i>
          <p>Không tìm thấy thương hiệu</p>
        </div>
      </div>
    );
  }

  return (
    <div className="chatbot-brand-list">
      <div className="chatbot-brand-list-container">
        {brands.map((brand) => (
          <BrandCard key={brand.id} brand={brand} />
        ))}
      </div>
      {totalPages > 1 && (
        <div className="chatbot-list-pagination">
          <span className="chatbot-page-info">
            Trang {currentPage + 1}/{totalPages}
          </span>
        </div>
      )}
    </div>
  );
};

export default BrandList;
