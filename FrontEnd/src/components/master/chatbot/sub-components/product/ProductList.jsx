import React from 'react';
import ProductCard from './ProductCard';
import './ProductList.css';

const ProductList = ({ data = {} }) => {
  const { data: products = [], currentPage = 0, totalPages = 1, pageSize = 10 } = data;

  if (!products || products.length === 0) {
    return (
      <div className="chatbot-product-list">
        <div className="empty-state">
          <i className="fa-solid fa-cube"></i>
          <p>Không tìm thấy sản phẩm</p>
        </div>
      </div>
    );
  }

  return (
    <div className="chatbot-product-list">
      <div className="product-list-container">
        {products.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>
      {totalPages > 1 && (
        <div className="list-pagination">
          <span className="page-info">
            Trang {currentPage + 1}/{totalPages}
          </span>
        </div>
      )}
    </div>
  );
};

export default ProductList;
