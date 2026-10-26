import { useNavigate } from 'react-router-dom';
import './OrderCard.css';

const OrderCard = ({ order }) => {
  const navigate = useNavigate();

  const handleCardClick = (e) => {
    e.stopPropagation();
    if (order.id) {
      navigate(`/orders/${order.id}`);
    }
  };

  // Format status with color
  const getStatusBadge = (status) => {
    const statusMap = {
      PENDING: { label: 'Chờ xử lý', color: '#FFA500' },
      CONFIRMED: { label: 'Đã xác nhận', color: '#4CAF50' },
      SHIPPED: { label: 'Đang giao', color: '#2196F3' },
      DELIVERED: { label: 'Đã giao', color: '#8BC34A' },
      CANCELLED: { label: 'Đã hủy', color: '#F44336' },
      RETURNED: { label: 'Đã hoàn', color: '#9C27B0' },
    };
    return statusMap[status] || { label: status, color: '#999' };
  };

  const statusBadge = getStatusBadge(order.status);
  const orderNum = order.id?.substring(0, 8).toUpperCase() || 'N/A';

  return (
    <div className="chatbot-order-card" onClick={handleCardClick} style={{ cursor: 'pointer' }}>
      <div className="order-card-header">
        <div className="order-card-number">
          <span className="label">Đơn hàng</span>
          <span className="number">#{orderNum}</span>
        </div>
        <div className="order-card-status" style={{ backgroundColor: statusBadge.color }}>
          {statusBadge.label}
        </div>
      </div>

      <div className="order-card-body">
        <div className="order-card-info">
          <div className="info-row">
            <span className="info-label">Người nhận:</span>
            <span className="info-value">{order.name || 'N/A'}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Điện thoại:</span>
            <span className="info-value">{order.phone || 'N/A'}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Địa chỉ:</span>
            <span className="info-value address">{order.address || 'N/A'}</span>
          </div>
        </div>

        <div className="order-card-footer">
          <div className="total-amount">
            <span className="label">Tổng tiền</span>
            <span className="amount">
              {new Intl.NumberFormat('vi-VN', {
                style: 'currency',
                currency: 'VND',
              }).format(order.total || 0)}
            </span>
          </div>
          <div className="order-date">
            {new Date(order.createdAt).toLocaleDateString('vi-VN')}
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderCard;
