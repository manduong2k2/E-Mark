import React from 'react';
import CategoryCard from './CategoryCard';
import './CategoryList.css';

const CategoryList = ({ data = {} }) => {
  const { data: categories = [], currentPage = 0, totalPages = 1 } = data;

  if (!categories || categories.length === 0) {
    return (
      <div className="chatbot-category-list">
        <div className="chatbot-empty-state">
          <i className="fa-solid fa-tag"></i>
          <p>Không tìm thấy danh mục</p>
        </div>
      </div>
    );
  }

  return (
    <div className="chatbot-category-list">
      <div className="chatbot-category-list-container">
        {categories.map((category) => (
          <CategoryCard key={category.id} category={category} />
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

export default CategoryList;
