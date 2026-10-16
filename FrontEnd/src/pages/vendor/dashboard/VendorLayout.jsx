// /my-vendor — the seller's dashboard. Same design system as the admin area (admin-ui-*),
// themed by the store tokens. Child pages read the vendor through useVendor().
import { useCallback, useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useMatch } from 'react-router-dom';
import { vendorService } from '../../../services/vendorService';
import { EmptyState, Pill } from '../../admin/shared/AdminUi';
import { vendorStatus } from './vendorUtils';
import '../../admin/shared/admin-index.css';
import './VendorDashboard.css';

const TABS = [
  { to: '/my-vendor', end: true, icon: 'bi-speedometer2', label: 'Overview' },
  { to: '/my-vendor/products', icon: 'bi-box-seam', label: 'Products' },
  { to: '/my-vendor/orders', icon: 'bi-receipt', label: 'Orders' },
  { to: '/my-vendor/store', icon: 'bi-shop', label: 'Store settings' },
];

function StoreLogo({ vendor, size = 56 }) {
  if (vendor?.logo) return <img className="vendor-logo" src={vendor.logo} alt="" style={{ width: size, height: size }} />;
  return (
    <span className="vendor-logo vendor-logo--empty" style={{ width: size, height: size }} aria-hidden="true">
      <i className="bi bi-shop"></i>
    </span>
  );
}

function StatusNotice({ vendor }) {
  if (vendor.status === 'PENDING') {
    return (
      <div className="vendor-notice vendor-notice--warning">
        <i className="bi bi-hourglass-split"></i>
        <p>
          <strong>Your store is waiting for approval.</strong> You can already complete your store details;
          products can be added as soon as an admin activates the store.
        </p>
      </div>
    );
  }
  if (vendor.status === 'BANNED') {
    return (
      <div className="vendor-notice vendor-notice--danger">
        <i className="bi bi-slash-circle"></i>
        <p><strong>Your store has been banned.</strong> Its products are hidden. Please contact support.</p>
      </div>
    );
  }
  return null;
}

export default function VendorLayout() {
  const [vendor, setVendor] = useState(null);
  const [loading, setLoading] = useState(true);
  const creating = useMatch('/my-vendor/store/new');

  const reloadVendor = useCallback(async () => {
    try {
      const res = await vendorService.getMyVendor();
      setVendor(res.ok ? res.data?.data ?? null : null);
    } catch {
      setVendor(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    document.title = 'My Store';
    reloadVendor();
  }, [reloadVendor]);

  const context = { vendor, loading, reloadVendor };

  if (loading) {
    return (
      <div className="admin-ui-page vendor-dashboard">
        <div className="admin-ui-card"><EmptyState icon="bi-hourglass-split" title="Loading your store…" /></div>
      </div>
    );
  }

  // No store yet: only the creation form is reachable
  if (!vendor) {
    return (
      <div className="admin-ui-page vendor-dashboard">
        {creating ? (
          <Outlet context={context} />
        ) : (
          <div className="admin-ui-card vendor-welcome">
            <span className="vendor-welcome-icon"><i className="bi bi-shop-window"></i></span>
            <h1>Open your store</h1>
            <p>
              Create a store to sell on the marketplace: add products with variants, manage stock
              and follow the orders of your customers.
            </p>
            <ol className="vendor-welcome-steps">
              <li><i className="bi bi-1-circle"></i> Fill in your store details</li>
              <li><i className="bi bi-2-circle"></i> An admin reviews and activates the store</li>
              <li><i className="bi bi-3-circle"></i> Add your products and start selling</li>
            </ol>
            <Link to="/my-vendor/store/new" className="admin-ui-btn admin-ui-btn--primary">
              <i className="bi bi-plus-lg"></i> Create my store
            </Link>
          </div>
        )}
      </div>
    );
  }

  const status = vendorStatus(vendor.status);

  return (
    <div className="admin-ui-page vendor-dashboard">
      <header className="vendor-header">
        {vendor.banner && <img className="vendor-header-banner" src={vendor.banner} alt="" />}
        <div className="vendor-header-body">
          <StoreLogo vendor={vendor} />
          <div className="vendor-header-text">
            <span className="admin-ui-eyebrow"><i className="bi bi-shop"></i> My store</span>
            <h1>{vendor.name}</h1>
            <div className="vendor-header-meta">
              <Pill tone={status.tone}>{status.label}</Pill>
              {vendor.email && <span className="admin-ui-muted"><i className="bi bi-envelope"></i> {vendor.email}</span>}
            </div>
          </div>
          <div className="admin-ui-header-actions">
            <Link to={`/vendor/${vendor.id}`} className="admin-ui-btn admin-ui-btn--ghost">
              <i className="bi bi-box-arrow-up-right"></i> View public page
            </Link>
          </div>
        </div>
        <nav className="vendor-tabs" aria-label="Store sections">
          {TABS.map(tab => (
            <NavLink key={tab.to} to={tab.to} end={tab.end} className={({ isActive }) => `vendor-tab${isActive ? ' is-active' : ''}`}>
              <i className={`bi ${tab.icon}`}></i> {tab.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <StatusNotice vendor={vendor} />

      <Outlet context={context} />
    </div>
  );
}
