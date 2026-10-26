import React from 'react';
import VendorCard from './VendorCard';
import './VendorList.css';

const VendorList = ({ data = {} }) => {
  const { data: vendors = [], currentPage = 0, totalPages = 1 } = data;

  if (!vendors || vendors.length === 0) {
    return (
      <div className="chatbot-vendor-list">
        <div className="chatbot-empty-state">
          <i className="fa-solid fa-store"></i>
          <p>Không tìm thấy nhà cung cấp</p>
        </div>
      </div>
    );
  }

  return (
    <div className="chatbot-vendor-list">
      <div className="chatbot-vendor-list-container">
        {vendors.map((vendor) => (
          <VendorCard key={vendor.id} vendor={vendor} />
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

export default VendorList;
