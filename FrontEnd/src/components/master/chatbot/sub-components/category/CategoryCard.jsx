import React from 'react';
import './CategoryCard.css';

const CategoryCard = ({ category }) => {
  return (
    <div className="chatbot-category-card">
      <div className="category-card-image">
        {category.image ? (
          <img src={category.image} alt={category.name} />
        ) : (
          <div className="placeholder">
            <i className="fa-solid fa-tag"></i>
          </div>
        )}
      </div>
      <div className="category-card-info">
        <h4 className="category-card-name">{category.name}</h4>
        {category.description && (
          <p className="category-card-description">{category.description}</p>
        )}
        {category.children && category.children.length > 0 && (
          <p className="category-card-children">
            {category.children.length} danh mục con
          </p>
        )}
      </div>
    </div>
  );
};

export default CategoryCard;
