// Sales → Orders → Order detail
import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { orderService } from '../../../services/orderService';
import { PageHeader, Avatar, Pill, EmptyState } from '../shared/AdminUi';
import { useApiQuery, formatDateTime, formatCurrency } from '../shared/adminUiUtils';
import defaultProductImage from '../../../assets/product.png';
import './AdminOrderDetailPage.css';
import { shortId } from '../../../utils/ids';
import { orderStatus } from '../../../utils/orderStatus';

function OrderStatusPill({ status }) {
  const { label, tone } = orderStatus(status);
  return <Pill tone={tone}>{label}</Pill>;
}


function AdminOrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const { response, loading } = useApiQuery(() => orderService.getOrderById(id), id);
  const order = response?.ok && response.data ? response.data : null;
  const error = !loading && !order ? (response ? 'Order not found' : 'Failed to load order details') : null;

  useEffect(() => {
    document.title = order ? `Order #${shortId(order.id)} - Admin` : 'Order - Admin';
  }, [order]);

  const backButton = (
    <button type="button" className="admin-ui-btn admin-ui-btn--ghost" onClick={() => navigate('/admin/orders')}>
      <i className="bi bi-arrow-left"></i> Back to orders
    </button>
  );

  if (loading && !order) {
    return (
      <div className="admin-ui-page">
        <PageHeader eyebrow="Sales" eyebrowIcon="bi-receipt" title="Order" description="Loading order details…" actions={backButton} />
        <div className="admin-order-detail-grid">
          <div className="admin-ui-card admin-order-detail-skeleton"><span></span></div>
          <div className="admin-ui-card admin-order-detail-skeleton"><span></span></div>
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="admin-ui-page">
        <PageHeader eyebrow="Sales" eyebrowIcon="bi-receipt" title="Order" actions={backButton} />
        <div className="admin-ui-card">
          <EmptyState icon="bi-exclamation-octagon" title={error || 'Order not found'} text="The order may have been removed or the link is wrong." />
        </div>
      </div>
    );
  }

  const items = order.items || [];
  const itemCount = items.reduce((sum, item) => sum + (item.quantity || 0), 0);

  return (
    <div className="admin-ui-page">
      <PageHeader
        eyebrow="Sales"
        eyebrowIcon="bi-receipt"
        title={`Order #${shortId(order.id)}`}
        description={`Placed ${formatDateTime(order.createdAt)}${order.name ? ` by ${order.name}` : ''}`}
        actions={backButton}
      />

      <div className="admin-order-detail-grid">
        {/* Items + summary */}
        <section className="admin-ui-card">
          <header className="admin-order-detail-card-head">
            <h2>Items</h2>
            <span className="admin-ui-muted">{itemCount} {itemCount === 1 ? 'item' : 'items'}</span>
          </header>
          <div className="admin-ui-table-wrap">
            <table className="admin-ui-table admin-order-detail-items">
              <thead>
                <tr>
                  <th>Product</th>
                  <th className="admin-order-detail-num">Price</th>
                  <th className="admin-order-detail-num">Qty</th>
                  <th className="admin-order-detail-num">Total</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => {
                  const snap = item.snapShot || item;
                  const image = snap.productImages?.[0] || defaultProductImage;
                  return (
                    <tr key={item.id}>
                      <td>
                        <div className="admin-order-detail-product">
                          <img src={image} alt={snap.productName || 'Product'} />
                          <div>
                            <strong>{snap.productName || 'Product'}</strong>
                            {(snap.productCode || snap.productSku) && (
                              <div className="admin-ui-chips">
                                <span className="admin-ui-tag admin-ui-mono">{snap.productCode || snap.productSku}</span>
                              </div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="admin-order-detail-num">{formatCurrency(snap.price)}</td>
                      <td className="admin-order-detail-num">× {item.quantity}</td>
                      <td className="admin-order-detail-num admin-order-detail-strong">{formatCurrency(item.total)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            {items.length === 0 && <EmptyState icon="bi-box-seam" title="No items" text="This order has no items." />}
          </div>

          <dl className="admin-order-detail-summary">
            <div>
              <dt>Subtotal</dt>
              <dd>{formatCurrency(order.total)}</dd>
            </div>
            <div className="admin-order-detail-summary-total">
              <dt>Total</dt>
              <dd>{formatCurrency(order.total)}</dd>
            </div>
          </dl>
        </section>

        <div className="admin-order-detail-side">
          {/* Order info + status */}
          <section className="admin-ui-card">
            <header className="admin-order-detail-card-head">
              <h2>Order</h2>
              <OrderStatusPill status={order.status} />
            </header>
            <dl className="admin-order-detail-info">
              <div>
                <dt>Order ID</dt>
                <dd className="admin-ui-mono">{order.id}</dd>
              </div>
              <div>
                <dt>User ID</dt>
                <dd className="admin-ui-mono">{order.userId || '—'}</dd>
              </div>
              <div>
                <dt>Order date</dt>
                <dd>{formatDateTime(order.createdAt)}</dd>
              </div>
              <div>
                <dt>Last updated</dt>
                <dd>{formatDateTime(order.updatedAt)}</dd>
              </div>
            </dl>
          </section>

          {/* Customer & shipping */}
          <section className="admin-ui-card">
            <header className="admin-order-detail-card-head">
              <h2>Customer &amp; shipping</h2>
            </header>
            <div className="admin-order-detail-customer">
              <div className="admin-ui-user">
                <Avatar user={{ name: order.name, email: order.phone }} size={40} />
                <div>
                  <strong>{order.name || 'Unknown'}</strong>
                  <span>{order.phone || '—'}</span>
                </div>
              </div>
            </div>
            <dl className="admin-order-detail-info">
              <div>
                <dt><i className="bi bi-telephone"></i> Phone</dt>
                <dd>{order.phone || '—'}</dd>
              </div>
              <div>
                <dt><i className="bi bi-geo-alt"></i> Address</dt>
                <dd>{order.address || '—'}</dd>
              </div>
              {order.note && (
                <div>
                  <dt><i className="bi bi-chat-left-text"></i> Note</dt>
                  <dd className="admin-order-detail-note">{order.note}</dd>
                </div>
              )}
            </dl>
          </section>
        </div>
      </div>
    </div>
  );
}

export default AdminOrderDetailPage;
