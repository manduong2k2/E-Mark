import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { productService } from '../../../services/productService';
import { useApiQuery, formatCurrency } from '../../admin/shared/adminUiUtils';
import { EmptyState, Pill } from '../../admin/shared/AdminUi';
import { LOW_STOCK, canSell, useVendor } from './vendorUtils';
import { SAMPLE_ORDERS, TO_HANDLE, orderTotal } from './sampleOrders';

function StatCard({ icon, tone, label, value, hint, to }) {
  const body = (
    <>
      <span className={`vendor-stat-icon vendor-stat-icon--${tone}`}><i className={`bi ${icon}`}></i></span>
      <span className="vendor-stat-text">
        <span className="vendor-stat-label">{label}</span>
        <strong className="vendor-stat-value">{value}</strong>
        {hint && <span className="vendor-stat-hint">{hint}</span>}
      </span>
    </>
  );
  return to
    ? <Link to={to} className="admin-ui-card vendor-stat vendor-stat--link">{body}</Link>
    : <div className="admin-ui-card vendor-stat">{body}</div>;
}

export default function VendorOverviewPage() {
  const { vendor } = useVendor();

  const { response, loading } = useApiQuery(
    () => productService.getAll({ vendorId: vendor.id, page: 0, size: 999 }),
    `vendor-products-${vendor.id}`
  );
  const products = useMemo(() => (response?.ok ? response.data?.data || [] : []), [response]);

  const stats = useMemo(() => {
    const stocks = products.map(p => (p.variants || []).reduce((sum, v) => sum + (Number(v.stock) || 0), 0));
    return {
      total: products.length,
      published: products.filter(p => p.status === 'PUBLISHED').length,
      lowStock: stocks.filter(stock => stock <= LOW_STOCK).length,
    };
  }, [products]);

  const pendingOrders = SAMPLE_ORDERS.filter(o => TO_HANDLE.includes(o.status)).length;
  const revenue = SAMPLE_ORDERS.filter(o => o.status === 'COMPLETED').reduce((sum, o) => sum + orderTotal(o), 0);

  return (
    <div className="vendor-page">
      <div className="vendor-stats">
        <StatCard icon="bi-box-seam" tone="accent" label="Products" value={loading ? '…' : stats.total}
          hint={loading ? null : `${stats.published} published`} to="/my-vendor/products" />
        <StatCard icon="bi-exclamation-triangle" tone="warning" label="Low stock" value={loading ? '…' : stats.lowStock}
          hint={`${LOW_STOCK} units or fewer`} to="/my-vendor/products" />
        <StatCard icon="bi-receipt" tone="info" label="Orders to handle" value={pendingOrders} hint="Sample data" to="/my-vendor/orders" />
        <StatCard icon="bi-cash-coin" tone="success" label="Completed revenue" value={formatCurrency(revenue)} hint="Sample data" />
      </div>

      <div className="vendor-overview-grid">
        <section className="admin-ui-card">
          <div className="vendor-card-head">
            <h2><i className="bi bi-shop"></i> Store details</h2>
            <Link to="/my-vendor/store" className="admin-ui-btn admin-ui-btn--ghost admin-ui-btn--sm">
              <i className="bi bi-pencil"></i> Edit
            </Link>
          </div>
          <dl className="vendor-details">
            <div><dt>Name</dt><dd>{vendor.name}</dd></div>
            <div><dt>Email</dt><dd>{vendor.email || <span className="admin-ui-muted">Not provided</span>}</dd></div>
            <div><dt>Phone</dt><dd>{vendor.phone || <span className="admin-ui-muted">Not provided</span>}</dd></div>
            <div><dt>Tax code</dt><dd>{vendor.taxCode ? <code>{vendor.taxCode}</code> : <span className="admin-ui-muted">Not provided</span>}</dd></div>
            <div className="vendor-details-wide">
              <dt>Description</dt>
              <dd>{vendor.description || <span className="admin-ui-muted">No description yet — tell customers what you sell.</span>}</dd>
            </div>
          </dl>
        </section>

        <section className="admin-ui-card">
          <div className="vendor-card-head">
            <h2><i className="bi bi-lightning-charge"></i> Quick actions</h2>
          </div>
          <div className="vendor-actions">
            {canSell(vendor) ? (
              <Link to="/my-vendor/products/new" className="vendor-action">
                <i className="bi bi-plus-square"></i>
                <span><strong>Add a product</strong><span>Variants, prices, stock and images</span></span>
              </Link>
            ) : (
              <div className="vendor-action is-disabled" aria-disabled="true">
                <i className="bi bi-plus-square"></i>
                <span><strong>Add a product</strong><span>Available once your store is approved</span></span>
              </div>
            )}
            <Link to="/my-vendor/orders" className="vendor-action">
              <i className="bi bi-truck"></i>
              <span><strong>Process orders</strong><span>{pendingOrders} waiting to be handled</span></span>
            </Link>
            <Link to="/my-vendor/store" className="vendor-action">
              <i className="bi bi-image"></i>
              <span><strong>Update branding</strong><span>Logo, banner and contact details</span></span>
            </Link>
          </div>
        </section>
      </div>

      {!loading && products.length === 0 && canSell(vendor) && (
        <div className="admin-ui-card">
          <EmptyState icon="bi-box-seam" title="No products yet" text="Add your first product to start selling.">
            <Link to="/my-vendor/products/new" className="admin-ui-btn admin-ui-btn--primary vendor-empty-action">
              <i className="bi bi-plus-lg"></i> Add product
            </Link>
          </EmptyState>
        </div>
      )}

      {stats.lowStock > 0 && (
        <p className="vendor-footnote">
          <Pill tone="warning">{stats.lowStock} low stock</Pill> Restock soon to avoid missing orders.
        </p>
      )}
    </div>
  );
}
