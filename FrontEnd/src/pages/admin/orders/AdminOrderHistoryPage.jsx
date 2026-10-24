// Sales → Orders
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { orderService } from '../../../services/orderService';
import { PageHeader, SearchBox, Avatar, Pill, Pagination, EmptyState } from '../shared/AdminUi';
import { PAGE_SIZE, useDebounce, useApiQuery, formatDate, formatCurrency } from '../shared/adminUiUtils';
import './AdminOrderHistoryPage.css';
import { shortId } from '../../../utils/ids';
import { ORDER_STATUSES, orderStatus } from '../../../utils/orderStatus';

const SORT_LABELS = { createdAt: 'Date', total: 'Total' };

function OrderStatusPill({ status }) {
  const { label, tone } = orderStatus(status);
  return <Pill tone={tone}>{label}</Pill>;
}

const itemName = (item) => item.snapShot?.name || item.snapShot?.productName || item.productName || 'Product';

function AdminOrderHistoryPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({
    status: '',
    search: '',
    sortBy: 'createdAt',
    sortOrder: 'desc',
  });
  const debouncedSearch = useDebounce(filters.search);

  useEffect(() => { document.title = 'Admin - Orders'; }, []);

  const params = { page, size: PAGE_SIZE, sortBy: filters.sortBy, sortOrder: filters.sortOrder };
  if (filters.status) params.status = filters.status;
  if (debouncedSearch) params.search = debouncedSearch;

  const { response, loading } = useApiQuery(() => orderService.getAllOrders(params), JSON.stringify(params));
  const ok = response?.ok && response.data;
  const orders = ok && Array.isArray(response.data.data) ? response.data.data : [];
  // Paging fields sit at the top level of this endpoint's response
  const currentPage = ok ? response.data.currentPage || 0 : 0;
  const totalPages = ok ? response.data.totalPages || 0 : 0;
  const pagination = ok ? {
    currentPage,
    totalPages,
    pageSize: response.data.pageSize || PAGE_SIZE,
    totalElements: response.data.totalElements || 0,
    hasNext: response.data.hasNext ?? currentPage < totalPages - 1,
    hasPrevious: response.data.hasPrevious ?? currentPage > 0,
  } : null;
  const error = !loading && !ok ? 'Failed to load orders' : null;

  const handleSort = (field) => {
    setFilters(prev => ({
      ...prev,
      sortBy: field,
      sortOrder: prev.sortBy === field && prev.sortOrder === 'desc' ? 'asc' : 'desc',
    }));
    setPage(0);
  };

  const updateFilter = (field) => (value) => {
    setFilters(prev => ({ ...prev, [field]: value }));
    setPage(0);
  };

  const openOrder = (order) => navigate(`/admin/orders/${order.id}`);

  return (
    <div className="admin-ui-page">
      <PageHeader
        eyebrow="Sales"
        eyebrowIcon="bi-receipt"
        title="Orders"
        description="Track every order placed on the marketplace, from checkout to delivery."
      />

      <div className="admin-ui-toolbar">
        <SearchBox
          value={filters.search}
          onChange={updateFilter('search')}
          placeholder="Search by name, phone, or order ID…"
        />

        <select
          className="admin-ui-select"
          value={filters.status}
          onChange={(e) => updateFilter('status')(e.target.value)}
          aria-label="Status filter"
        >
          <option value="">All statuses</option>
          {Object.entries(ORDER_STATUSES).map(([value, { label }]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>

        <div className="admin-ui-segmented admin-orders-sort" role="group" aria-label="Sort by">
          {Object.entries(SORT_LABELS).map(([field, label]) => {
            const active = filters.sortBy === field;
            return (
              <button key={field} type="button" className={active ? 'active' : ''} onClick={() => handleSort(field)}>
                {label}
                {active && <i className={`bi ${filters.sortOrder === 'asc' ? 'bi-arrow-up' : 'bi-arrow-down'}`}></i>}
              </button>
            );
          })}
        </div>
      </div>

      <div className="admin-ui-card">
        <div className="admin-ui-table-wrap">
          <table className="admin-ui-table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Customer</th>
                <th>Items</th>
                <th>Total</th>
                <th>Status</th>
                <th>Date</th>
                <th className="admin-ui-col-actions"><span className="visually-hidden">Open</span></th>
              </tr>
            </thead>
            <tbody>
              {loading && orders.length === 0 && [...Array(5)].map((_, i) => (
                <tr key={i} className="admin-ui-skeleton-row"><td colSpan={7}><span></span></td></tr>
              ))}
              {orders.map(order => {
                const items = order.items || [];
                const count = items.reduce((sum, item) => sum + (item.quantity || 0), 0);
                return (
                  <tr key={order.id} onClick={() => openOrder(order)}>
                    <td>
                      <span className="admin-ui-mono admin-orders-id">#{shortId(order.id)}</span>
                      {order.userId && <span className="admin-orders-sub admin-ui-mono">User {shortId(order.userId)}</span>}
                    </td>
                    <td>
                      <div className="admin-ui-user">
                        <Avatar user={{ name: order.name, email: order.phone }} size={32} />
                        <div>
                          <strong>{order.name || 'Unknown'}</strong>
                          <span>{order.phone || '—'}</span>
                        </div>
                      </div>
                    </td>
                    <td className="admin-orders-items">
                      {items.length === 0 ? (
                        <span className="admin-ui-muted">—</span>
                      ) : (
                        <>
                          <strong>{count} {count === 1 ? 'item' : 'items'}</strong>
                          <span className="admin-orders-sub">
                            {items.slice(0, 2).map(item => `${item.quantity}× ${itemName(item)}`).join(', ')}
                            {items.length > 2 && ` +${items.length - 2} more`}
                          </span>
                        </>
                      )}
                    </td>
                    <td className="admin-orders-total">{formatCurrency(order.total)}</td>
                    <td><OrderStatusPill status={order.status} /></td>
                    <td className="admin-ui-muted">{formatDate(order.createdAt)}</td>
                    <td className="admin-ui-col-actions">
                      <span className="admin-ui-icon-btn admin-orders-open" aria-hidden="true">
                        <i className="bi bi-chevron-right"></i>
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {error && <EmptyState icon="bi-exclamation-octagon" title={error} text="Check your connection and try again." />}
          {!loading && !error && orders.length === 0 && (
            <EmptyState icon="bi-box-seam" title="No orders found" text="There are no orders matching your criteria." />
          )}
        </div>
        <Pagination pagination={pagination} onChange={setPage} />
      </div>
    </div>
  );
}


export default AdminOrderHistoryPage;
