import React from 'react';
import OrderCard from './OrderCard';
import './OrderList.css';

const OrderList = ({ data = {} }) => {
  const { data: orders = [], currentPage = 0, totalPages = 1, pageSize = 5 } = data;

  if (!orders || orders.length === 0) {
    return (
      <div className="chatbot-order-list">
        <div className="chatbot-empty-state">
          <i className="fa-solid fa-receipt"></i>
          <p>Không có đơn hàng nào</p>
        </div>
      </div>
    );
  }

  return (
    <div className="chatbot-order-list">
      <div className="chatbot-order-list-container">
        {orders.map((order) => (
          <OrderCard key={order.id} order={order} />
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

export default OrderList;
