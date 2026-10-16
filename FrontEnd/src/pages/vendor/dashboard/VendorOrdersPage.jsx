// /my-vendor/orders — the store's orders. Sample data (sampleOrders.js) until a vendor orders API exists:
// status changes only update this page's state.
import { useEffect, useMemo, useState } from 'react';
import { Drawer, EmptyState, Pagination, Pill, SearchBox } from '../../admin/shared/AdminUi';
import { formatCurrency, formatDateTime } from '../../admin/shared/adminUiUtils';
import { matchesSearch } from '../../../components/ui/uiUtils';
import { ORDER_STATUSES, orderStatus } from '../../../utils/orderStatus';
import { shortId } from '../../../utils/ids';
import { CANCELLABLE, NEXT_STATUS, SAMPLE_ORDERS, TO_HANDLE, orderTotal } from './sampleOrders';

const PAGE_SIZE = 10;

function StatusPill({ status }) {
  const { label, tone } = orderStatus(status);
  return <Pill tone={tone}>{label}</Pill>;
}

function OrderDrawer({ order, onClose, onChangeStatus }) {
  const next = NEXT_STATUS[order.status];
  const status = orderStatus(order.status);
  const units = order.items.reduce((sum, line) => sum + line.quantity, 0);

  return (
    <Drawer label={`Order #${shortId(order.id)}`} onClose={onClose} width={480}>
      <div className="admin-ui-drawer-hero">
        <button type="button" className="admin-ui-icon-btn admin-ui-drawer-close" onClick={onClose} aria-label="Close">
          <i className="bi bi-x-lg"></i>
        </button>
        <span className={`vendor-order-icon vendor-order-icon--${status.tone}`}><i className={`bi ${status.icon}`}></i></span>
        <h2>Order <span className="admin-ui-mono">#{shortId(order.id)}</span></h2>
        <div className="admin-ui-drawer-meta">
          <StatusPill status={order.status} />
          <span className="admin-ui-muted"><i className="bi bi-calendar3"></i> {formatDateTime(order.createdAt)}</span>
        </div>
      </div>

      {(next || CANCELLABLE.includes(order.status)) && (
        <section className="admin-ui-drawer-section">
          <h3>Next step</h3>
          <div className="vendor-order-actions">
            {next && (
              <button type="button" className="admin-ui-btn admin-ui-btn--primary" onClick={() => onChangeStatus(order, next.status)}>
                <i className={`bi ${next.icon}`}></i> {next.label}
              </button>
            )}
            {CANCELLABLE.includes(order.status) && (
              <button type="button" className="admin-ui-btn admin-ui-btn--ghost admin-ui-btn--danger-text" onClick={() => onChangeStatus(order, 'CANCELLED')}>
                <i className="bi bi-x-circle"></i> Cancel order
              </button>
            )}
          </div>
        </section>
      )}

      <section className="admin-ui-drawer-section">
        <h3>Customer</h3>
        <dl className="vendor-details vendor-details--stacked">
          <div><dt>Recipient</dt><dd>{order.name}</dd></div>
          <div><dt>Phone</dt><dd><a href={`tel:${order.phone.replace(/\s/g, '')}`}>{order.phone}</a></dd></div>
          <div><dt>Delivery address</dt><dd>{order.address}</dd></div>
          {order.note && <div><dt>Note</dt><dd className="vendor-order-note">{order.note}</dd></div>}
        </dl>
      </section>

      <section className="admin-ui-drawer-section">
        <h3>Items · {units}</h3>
        <ul className="vendor-order-lines">
          {order.items.map(line => (
            <li key={line.id}>
              <span className="vendor-thumb vendor-thumb--empty vendor-thumb--sm" aria-hidden="true"><i className="bi bi-box"></i></span>
              <div>
                <strong>{line.productName}</strong>
                <span className="admin-ui-muted"><code>{line.productSku}</code> · {formatCurrency(line.price)} × {line.quantity}</span>
              </div>
              <span className="vendor-nowrap">{formatCurrency(line.total)}</span>
            </li>
          ))}
        </ul>
        <div className="vendor-order-total">
          <span>Total</span>
          <strong>{formatCurrency(orderTotal(order))}</strong>
        </div>
      </section>

      <section className="admin-ui-drawer-section">
        <h3>Timeline</h3>
        <dl className="vendor-details">
          <div><dt>Placed</dt><dd>{formatDateTime(order.createdAt)}</dd></div>
          <div><dt>Last update</dt><dd>{formatDateTime(order.updatedAt)}</dd></div>
        </dl>
      </section>
    </Drawer>
  );
}

export default function VendorOrdersPage() {
  useEffect(() => { document.title = 'My Store - Orders'; }, []);

  const [orders, setOrders] = useState(SAMPLE_ORDERS);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('TO_HANDLE');
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState(null);

  const counts = useMemo(() => {
    const result = { ALL: orders.length, TO_HANDLE: orders.filter(o => TO_HANDLE.includes(o.status)).length };
    orders.forEach(o => { result[o.status] = (result[o.status] || 0) + 1; });
    return result;
  }, [orders]);

  // "To handle" first, then every status that has orders
  const filters = useMemo(() => [
    ['TO_HANDLE', 'To handle'],
    ['ALL', 'All'],
    ...Object.entries(ORDER_STATUSES).filter(([key]) => counts[key]).map(([key, { label }]) => [key, label]),
  ], [counts]);

  const filtered = useMemo(() => orders
    .filter(o => status === 'ALL' || (status === 'TO_HANDLE' ? TO_HANDLE.includes(o.status) : o.status === status))
    .filter(o => matchesSearch(`${shortId(o.id)} ${o.name} ${o.phone} ${o.items.map(i => i.productName).join(' ')}`, search))
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)), [orders, status, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const rows = filtered.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const pagination = {
    currentPage: current, totalPages, totalElements: filtered.length, pageSize: PAGE_SIZE,
    hasNext: current < totalPages - 1, hasPrevious: current > 0,
  };

  const selected = orders.find(o => o.id === selectedId) || null;

  const changeStatus = (order, nextStatus) => {
    setOrders(list => list.map(o => (o.id === order.id ? { ...o, status: nextStatus, updatedAt: new Date().toISOString() } : o)));
    window.showSuccess(`Order #${shortId(order.id)} is now ${orderStatus(nextStatus).label.toLowerCase()}`);
  };

  const changeFilter = (setter) => (value) => { setter(value); setPage(0); };

  return (
    <div className="vendor-page">
      <div className="vendor-section-head">
        <div>
          <h2>Orders</h2>
          <p className="admin-ui-muted">Orders containing your products. Move each one forward as you prepare and ship it.</p>
        </div>
      </div>

      <div className="vendor-notice vendor-notice--info">
        <i className="bi bi-info-circle"></i>
        <p><strong>Sample data.</strong> These orders are examples; status changes are not saved yet.</p>
      </div>

      <div className="admin-ui-toolbar">
        <SearchBox value={search} onChange={changeFilter(setSearch)} placeholder="Search by order #, customer, phone or product…" />
        <div className="admin-ui-segmented" role="tablist" aria-label="Filter by status">
          {filters.map(([value, label]) => (
            <button key={value} type="button" className={status === value ? 'active' : ''} onClick={() => changeFilter(setStatus)(value)}>
              {label} <span className="vendor-count">{counts[value] || 0}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="admin-ui-card">
        {filtered.length === 0 ? (
          <EmptyState
            icon={status === 'TO_HANDLE' ? 'bi-check2-all' : 'bi-receipt'}
            title={status === 'TO_HANDLE' ? 'All caught up' : 'No matching orders'}
            text={status === 'TO_HANDLE' ? 'There is no order waiting for you.' : 'Try another search or status.'}
          />
        ) : (
          <>
            <div className="admin-ui-table-wrap">
              <table className="admin-ui-table">
                <thead>
                  <tr>
                    <th>Order</th>
                    <th>Customer</th>
                    <th>Items</th>
                    <th>Total</th>
                    <th>Status</th>
                    <th className="admin-ui-col-actions"><span className="visually-hidden">Open</span></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map(order => (
                    <tr key={order.id} className={order.id === selectedId ? 'is-selected' : ''} onClick={() => setSelectedId(order.id)}>
                      <td>
                        <div className="vendor-stack">
                          <strong className="admin-ui-mono">#{shortId(order.id)}</strong>
                          <span className="admin-ui-muted">{formatDateTime(order.createdAt)}</span>
                        </div>
                      </td>
                      <td>
                        <div className="vendor-stack">
                          <strong>{order.name}</strong>
                          <span className="admin-ui-muted">{order.phone}</span>
                        </div>
                      </td>
                      <td>
                        <div className="vendor-stack">
                          <span>{order.items[0].productName}</span>
                          {order.items.length > 1 && <span className="admin-ui-muted">+{order.items.length - 1} more</span>}
                        </div>
                      </td>
                      <td className="vendor-nowrap"><strong>{formatCurrency(orderTotal(order))}</strong></td>
                      <td><StatusPill status={order.status} /></td>
                      <td className="admin-ui-col-actions">
                        <span className="admin-ui-icon-btn" aria-hidden="true"><i className="bi bi-chevron-right"></i></span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination pagination={pagination} onChange={setPage} />
          </>
        )}
      </div>

      {selected && <OrderDrawer order={selected} onClose={() => setSelectedId(null)} onChangeStatus={changeStatus} />}
    </div>
  );
}
