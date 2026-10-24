// /my-vendor/products — the store's products (admin-style table).
import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { productService } from '../../../services/productService';
import { ConfirmDialog, EmptyState, Pagination, Pill, SearchBox } from '../../admin/shared/AdminUi';
import { formatCurrency, useApiQuery } from '../../admin/shared/adminUiUtils';
import { matchesSearch } from '../../../components/ui/uiUtils';
import { LOW_STOCK, PRODUCT_STATUS, canSell, imageUrl, useVendor } from './vendorUtils';

const PAGE_SIZE = 10;
const FILTERS = [['ALL', 'All'], ['PUBLISHED', 'Published'], ['DRAFT', 'Draft'], ['ARCHIVED', 'Archived']];

const totalStock = (variants = []) => variants.reduce((sum, v) => sum + (Number(v.stock) || 0), 0);

const priceRange = (variants = []) => {
  const prices = variants.map(v => Number(v.price)).filter(p => !Number.isNaN(p));
  if (!prices.length) return '—';
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  return min === max ? formatCurrency(min) : `${formatCurrency(min)} – ${formatCurrency(max)}`;
};

function StockPill({ stock }) {
  if (stock <= 0) return <Pill tone="danger">Out of stock</Pill>;
  if (stock <= LOW_STOCK) return <Pill tone="warning">Low · {stock}</Pill>;
  return <Pill tone="success">In stock · {stock}</Pill>;
}

function ProductThumb({ product }) {
  const src = imageUrl(product.variants?.find(v => v.images?.length)?.images?.[0]);
  return src
    ? <img className="vendor-thumb" src={src} alt="" loading="lazy" />
    : <span className="vendor-thumb vendor-thumb--empty" aria-hidden="true"><i className="bi bi-image"></i></span>;
}

export default function VendorProductsPage() {
  const { vendor } = useVendor();
  const navigate = useNavigate();
  const sellable = canSell(vendor);

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const [page, setPage] = useState(0);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  // A store's catalog is small: load it once, filter and paginate in the browser
  const { response, loading, reload } = useApiQuery(
    () => productService.getAll({ vendorId: vendor.id, page: 0, size: 999 }),
    `vendor-products-${vendor.id}`
  );
  const products = useMemo(() => (response?.ok ? response.data?.data || [] : []), [response]);
  const failed = !loading && response && !response.ok;

  const counts = useMemo(() => {
    const result = { ALL: products.length };
    products.forEach(p => { result[p.status] = (result[p.status] || 0) + 1; });
    return result;
  }, [products]);

  const filtered = useMemo(() => products.filter(p =>
    (status === 'ALL' || p.status === status)
    && matchesSearch(`${p.name} ${p.brand?.name || ''} ${(p.variants || []).map(v => v.name).join(' ')}`, search)
  ), [products, status, search]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const rows = filtered.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const pagination = {
    currentPage: current, totalPages, totalElements: filtered.length, pageSize: PAGE_SIZE,
    hasNext: current < totalPages - 1, hasPrevious: current > 0,
  };

  const changeFilter = (setter) => (value) => { setter(value); setPage(0); };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      const res = await productService.delete(toDelete.id);
      if (res.ok) {
        window.showSuccess(`"${toDelete.name}" deleted`);
        reload();
      }
    } finally {
      setDeleting(false);
      setToDelete(null);
    }
  };

  const addButton = sellable ? (
    <Link to="/my-vendor/products/new" className="admin-ui-btn admin-ui-btn--primary">
      <i className="bi bi-plus-lg"></i> Add product
    </Link>
  ) : (
    <button type="button" className="admin-ui-btn admin-ui-btn--primary" disabled title="Available once your store is approved">
      <i className="bi bi-plus-lg"></i> Add product
    </button>
  );

  return (
    <div className="vendor-page">
      <div className="vendor-section-head">
        <div>
          <h2>Products</h2>
          <p className="admin-ui-muted">Everything your store sells. Customers only see published products.</p>
        </div>
        {addButton}
      </div>

      <div className="admin-ui-toolbar">
        <SearchBox value={search} onChange={changeFilter(setSearch)} placeholder="Search by name, brand or variant…" />
        <div className="admin-ui-segmented" role="tablist" aria-label="Filter by status">
          {FILTERS.map(([value, label]) => (
            <button key={value} type="button" className={status === value ? 'active' : ''} onClick={() => changeFilter(setStatus)(value)}>
              {label} <span className="vendor-count">{counts[value] || 0}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="admin-ui-card">
        {loading && !response ? (
          <EmptyState icon="bi-hourglass-split" title="Loading products…" />
        ) : failed ? (
          <EmptyState icon="bi-exclamation-triangle" title="Could not load your products" text="Please try again later." />
        ) : products.length === 0 ? (
          <EmptyState
            icon="bi-box-seam"
            title="No products yet"
            text={sellable ? 'Add your first product with its variants, prices and images.' : 'You can add products once your store is approved.'}
          >
            {sellable && <div className="vendor-empty-action">{addButton}</div>}
          </EmptyState>
        ) : filtered.length === 0 ? (
          <EmptyState icon="bi-search" title="No matching products" text="Try another search or status." />
        ) : (
          <>
            <div className="admin-ui-table-wrap">
              <table className="admin-ui-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Variants</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th>Status</th>
                    <th className="admin-ui-col-actions"><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map(product => {
                    const productStatus = PRODUCT_STATUS[product.status] || { label: product.status, tone: 'neutral' };
                    return (
                      <tr key={product.id} onClick={() => navigate(`/my-vendor/products/${product.id}/edit`)}>
                        <td>
                          <div className="vendor-product-cell">
                            <ProductThumb product={product} />
                            <div>
                              <strong>{product.name}</strong>
                              <span className="admin-ui-muted">{product.brand?.name || 'No brand'}</span>
                            </div>
                          </div>
                        </td>
                        <td>{product.variants?.length || 0}</td>
                        <td className="vendor-nowrap">{priceRange(product.variants)}</td>
                        <td><StockPill stock={totalStock(product.variants)} /></td>
                        <td><Pill tone={productStatus.tone}>{productStatus.label}</Pill></td>
                        <td className="admin-ui-col-actions" onClick={(e) => e.stopPropagation()}>
                          <Link to={`/product/${product.id}`} className="admin-ui-icon-btn" title="View in store" aria-label="View in store">
                            <i className="bi bi-eye"></i>
                          </Link>
                          <Link to={`/my-vendor/products/${product.id}/edit`} className="admin-ui-icon-btn" title="Edit" aria-label="Edit product">
                            <i className="bi bi-pencil"></i>
                          </Link>
                          <button type="button" className="admin-ui-icon-btn admin-ui-icon-btn--danger" title="Delete" aria-label="Delete product"
                            onClick={() => setToDelete(product)}>
                            <i className="bi bi-trash"></i>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <Pagination pagination={pagination} onChange={setPage} />
          </>
        )}
      </div>

      {toDelete && (
        <ConfirmDialog
          title="Delete product"
          message={<>Delete <strong>{toDelete.name}</strong> and its {toDelete.variants?.length || 0} variant(s)? It will no longer be shown in the store.</>}
          busy={deleting}
          onConfirm={confirmDelete}
          onClose={() => setToDelete(null)}
        />
      )}
    </div>
  );
}
