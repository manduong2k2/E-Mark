// src/pages/admin/products/AdminProductsPage.jsx
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productService } from '../../../services/productService';
import { PageHeader, SearchBox, Pill, Pagination, EmptyState, Modal, ConfirmDialog } from '../shared/AdminUi';
import { useApiQuery, formatCurrency } from '../shared/adminUiUtils';
import './AdminProductsPage.css';

const STATUS_TONES = { PUBLISHED: 'success', DRAFT: 'neutral', ARCHIVED: 'warning' };
const STATUS_LABELS = { PUBLISHED: 'Published', DRAFT: 'Draft', ARCHIVED: 'Archived' };
const LOW_STOCK = 10;

// Variant images come either as plain URLs or as { url } objects
const imageUrl = (img) => (typeof img === 'string' ? img : img?.url);

const priceRange = (variants = []) => {
  const prices = variants.map(v => Number(v.price)).filter(p => !Number.isNaN(p));
  if (prices.length === 0) return '—';
  const min = Math.min(...prices);
  const max = Math.max(...prices);
  return min === max ? formatCurrency(min) : `${formatCurrency(min)} – ${formatCurrency(max)}`;
};

const totalStock = (variants = []) => variants.reduce((sum, v) => sum + (Number(v.stock) || 0), 0);

function StatusPill({ status }) {
  return <Pill tone={STATUS_TONES[status] || 'neutral'}>{STATUS_LABELS[status] || status || 'Unknown'}</Pill>;
}

function StockPill({ stock }) {
  if (stock <= 0) return <Pill tone="danger">Out of stock</Pill>;
  if (stock <= LOW_STOCK) return <Pill tone="warning">Low · {stock}</Pill>;
  return <Pill tone="success">In stock · {stock}</Pill>;
}

export default function AdminProductsPage() {
  useEffect(() => { document.title = 'Admin - Products'; }, []);

  const navigate = useNavigate();

  // Search & filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [brandFilter, setBrandFilter] = useState('ALL');

  // Pagination (client side)
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // Selection
  const [selectedIds, setSelectedIds] = useState(new Set());

  // Dialogs
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [detailsTarget, setDetailsTarget] = useState(null);
  const [detailProduct, setDetailProduct] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  // Fetch all products once (client-side filtering)
  const { response, loading, reload } = useApiQuery(() => productService.getAll({ page: 0, size: 999 }), 'all-products');
  const allProducts = useMemo(() => response?.data?.data || [], [response]);
  const error = !loading && (!response || response.ok === false) ? 'Failed to load products' : null;

  const brands = useMemo(() => {
    const unique = new Set();
    allProducts.forEach(p => { if (p.brand?.name) unique.add(p.brand.name); });
    return Array.from(unique).sort();
  }, [allProducts]);

  const filteredProducts = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return allProducts.filter(p => {
      if (q && !(
        p.name?.toLowerCase().includes(q) ||
        p.id?.toLowerCase().includes(q) ||
        p.brand?.name?.toLowerCase().includes(q)
      )) return false;
      if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
      if (brandFilter !== 'ALL' && p.brand?.name !== brandFilter) return false;
      return true;
    });
  }, [allProducts, searchQuery, statusFilter, brandFilter]);

  const stats = useMemo(() => ({
    ALL: allProducts.length,
    PUBLISHED: allProducts.filter(p => p.status === 'PUBLISHED').length,
    DRAFT: allProducts.filter(p => p.status === 'DRAFT').length,
    ARCHIVED: allProducts.filter(p => p.status === 'ARCHIVED').length,
  }), [allProducts]);

  const totalPages = Math.ceil(filteredProducts.length / pageSize) || 1;
  const page = Math.min(currentPage, totalPages - 1);
  const paginatedData = filteredProducts.slice(page * pageSize, (page + 1) * pageSize);
  const pagination = {
    currentPage: page,
    totalPages,
    totalElements: filteredProducts.length,
    pageSize,
    hasNext: page < totalPages - 1,
    hasPrevious: page > 0,
  };

  // Back to the first page whenever a filter changes
  const updateFilter = (setter) => (value) => { setter(value); setCurrentPage(0); };
  const hasFilters = searchQuery || statusFilter !== 'ALL' || brandFilter !== 'ALL';
  const resetFilters = () => {
    setSearchQuery('');
    setStatusFilter('ALL');
    setBrandFilter('ALL');
    setCurrentPage(0);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      const res = await productService.delete(deleteTarget.id);
      if (res.ok) {
        window.showSuccess('Product deleted successfully');
        reload();
      } else {
        window.showError(res.data?.message || 'Failed to delete product');
      }
    } catch {
      window.showError('Server connection error');
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  };

  const handleViewDetails = async (product) => {
    setDetailsTarget(product);
    setDetailProduct(null);
    setLoadingDetail(true);
    try {
      const res = await productService.getById(product.id);
      if (res.ok) {
        // Handle both response formats: { success: true, data: {...} } or direct {...}
        setDetailProduct(res.data?.data || res.data);
      } else {
        window.showError(res.data?.message || 'Failed to fetch product details');
        setDetailProduct(product); // Fallback to table data
      }
    } catch (err) {
      console.error('Failed to load product details:', err);
      window.showError('Server connection error');
      setDetailProduct(product); // Fallback to table data
    } finally {
      setLoadingDetail(false);
    }
  };

  const closeDetails = () => { setDetailsTarget(null); setDetailProduct(null); };

  const toggleSelectProduct = (id) => {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id); else next.add(id);
    setSelectedIds(next);
  };

  const allOnPageSelected = paginatedData.length > 0 && paginatedData.every(p => selectedIds.has(p.id));
  const toggleSelectAll = () => {
    setSelectedIds(allOnPageSelected ? new Set() : new Set(paginatedData.map(p => p.id)));
  };

  return (
    <div className="admin-ui-page">
      <PageHeader
        eyebrow="Catalog"
        eyebrowIcon="bi-box-seam"
        title="Products"
        description="Manage the catalog, variants, inventory and pricing."
        actions={
          <button type="button" className="admin-ui-btn admin-ui-btn--primary" onClick={() => navigate('/admin/products/create')}>
            <i className="bi bi-plus-lg"></i> New product
          </button>
        }
      />

      {error && (
        <div className="admin-products-alert" role="alert">
          <i className="bi bi-exclamation-octagon"></i>
          <span>{error}</span>
          <button type="button" className="admin-ui-btn admin-ui-btn--ghost admin-ui-btn--sm" onClick={reload}>
            <i className="bi bi-arrow-clockwise"></i> Retry
          </button>
        </div>
      )}

      <div className="admin-ui-toolbar">
        <SearchBox
          value={searchQuery}
          onChange={updateFilter(setSearchQuery)}
          placeholder="Search by name, product ID or brand…"
        />

        <div className="admin-ui-segmented" role="group" aria-label="Status filter">
          {[['ALL', 'All'], ['PUBLISHED', 'Published'], ['DRAFT', 'Draft'], ['ARCHIVED', 'Archived']].map(([value, label]) => (
            <button
              key={value}
              type="button"
              className={statusFilter === value ? 'active' : ''}
              onClick={() => updateFilter(setStatusFilter)(value)}
            >
              {label} <span className="admin-products-count">{stats[value]}</span>
            </button>
          ))}
        </div>

        <select className="admin-ui-select" value={brandFilter} onChange={(e) => updateFilter(setBrandFilter)(e.target.value)} aria-label="Brand filter">
          <option value="ALL">All brands</option>
          {brands.map(b => <option key={b} value={b}>{b}</option>)}
        </select>

        {hasFilters && (
          <button type="button" className="admin-ui-btn admin-ui-btn--ghost" onClick={resetFilters}>
            <i className="bi bi-arrow-counterclockwise"></i> Reset
          </button>
        )}

        <select
          className="admin-ui-select admin-products-page-size"
          value={pageSize}
          onChange={(e) => { setPageSize(Number(e.target.value)); setCurrentPage(0); }}
          aria-label="Rows per page"
        >
          <option value="10">10 / page</option>
          <option value="20">20 / page</option>
          <option value="50">50 / page</option>
        </select>
      </div>

      <div className="admin-ui-card">
        {selectedIds.size > 0 && (
          <div className="admin-products-selection">
            <span><strong>{selectedIds.size}</strong> selected</span>
            <button type="button" className="admin-ui-btn admin-ui-btn--ghost admin-ui-btn--sm" onClick={() => setSelectedIds(new Set())}>
              Clear selection
            </button>
          </div>
        )}

        <div className="admin-ui-table-wrap">
          <table className="admin-ui-table admin-products-table">
            <thead>
              <tr>
                <th className="admin-products-col-check">
                  <input
                    type="checkbox"
                    className="admin-products-check"
                    checked={allOnPageSelected}
                    onChange={toggleSelectAll}
                    aria-label="Select all on this page"
                  />
                </th>
                <th>Product</th>
                <th>Brand</th>
                <th>Categories</th>
                <th>Variants &amp; price</th>
                <th>Stock</th>
                <th>Status</th>
                <th className="admin-ui-col-actions"><span className="visually-hidden">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {loading && allProducts.length === 0 && [...Array(5)].map((_, i) => (
                <tr key={i} className="admin-ui-skeleton-row"><td colSpan={8}><span></span></td></tr>
              ))}
              {paginatedData.map(prod => {
                const isSelected = selectedIds.has(prod.id);
                const thumb = imageUrl(prod.variants?.[0]?.images?.[0]);
                const variantCount = prod.variants?.length || 0;

                return (
                  <tr key={prod.id} className={isSelected ? 'is-selected' : ''} onClick={() => handleViewDetails(prod)}>
                    <td className="admin-products-col-check" onClick={(e) => e.stopPropagation()}>
                      <input
                        type="checkbox"
                        className="admin-products-check"
                        checked={isSelected}
                        onChange={() => toggleSelectProduct(prod.id)}
                        aria-label={`Select ${prod.name}`}
                      />
                    </td>
                    <td>
                      <div className="admin-products-cell">
                        <span className="admin-products-thumb">
                          {thumb ? <img src={thumb} alt="" /> : <i className="bi bi-image"></i>}
                        </span>
                        <div className="admin-products-cell-text">
                          <strong title={prod.name}>{prod.name}</strong>
                          <span className="admin-ui-mono" title={prod.id}>{prod.id}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      {prod.brand?.name
                        ? <span className="admin-products-brand">{prod.brand.name}</span>
                        : <span className="admin-ui-muted">Uncategorized</span>}
                    </td>
                    <td>
                      {prod.categories?.length > 0 ? (
                        <div className="admin-ui-chips">
                          {prod.categories.map(cat => <span key={cat.id || cat.name} className="admin-ui-chip">{cat.name}</span>)}
                        </div>
                      ) : <span className="admin-ui-muted">—</span>}
                    </td>
                    <td>
                      <div className="admin-products-price">
                        <strong>{priceRange(prod.variants)}</strong>
                        <span className="admin-ui-muted"><i className="bi bi-layers"></i> {variantCount} variant{variantCount === 1 ? '' : 's'}</span>
                      </div>
                    </td>
                    <td><StockPill stock={totalStock(prod.variants)} /></td>
                    <td><StatusPill status={prod.status} /></td>
                    <td className="admin-ui-col-actions" onClick={(e) => e.stopPropagation()}>
                      <button type="button" className="admin-ui-icon-btn" title="View details" onClick={() => handleViewDetails(prod)}>
                        <i className="bi bi-eye"></i>
                      </button>
                      <button type="button" className="admin-ui-icon-btn" title="Edit" onClick={() => navigate(`/admin/products/edit/${prod.id}`)}>
                        <i className="bi bi-pencil"></i>
                      </button>
                      <button type="button" className="admin-ui-icon-btn admin-ui-icon-btn--danger" title="Delete" onClick={() => setDeleteTarget(prod)}>
                        <i className="bi bi-trash3"></i>
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {!loading && paginatedData.length === 0 && (
            <EmptyState
              icon="bi-box-seam"
              title={hasFilters ? 'No matching products' : 'No products yet'}
              text={hasFilters ? 'Try another search or adjust the filters.' : 'Use "New product" to add the first one.'}
            />
          )}
        </div>
        <Pagination pagination={pagination} onChange={setCurrentPage} />
      </div>

      {detailsTarget && (
        <ProductDetailsModal
          product={detailProduct}
          fallback={detailsTarget}
          loading={loadingDetail}
          onClose={closeDetails}
          onEdit={() => navigate(`/admin/products/edit/${detailsTarget.id}`)}
        />
      )}

      {deleteTarget && (
        <ConfirmDialog
          title="Delete product"
          message={<>Are you sure you want to delete product <strong>{deleteTarget.name}</strong>?</>}
          busy={deleting}
          onConfirm={handleDelete}
          onClose={() => setDeleteTarget(null)}
        />
      )}
    </div>
  );
}

// ── Product details (variants, brand, categories) ───────────────────────────
function ProductDetailsModal({ product, fallback, loading, onClose, onEdit }) {
  return (
    <Modal
      title={product?.name || fallback.name}
      subtitle={<span className="admin-ui-mono">{product?.id || fallback.id}</span>}
      onClose={onClose}
      width={880}
      footer={
        <>
          <button type="button" className="admin-ui-btn admin-ui-btn--ghost" onClick={onClose}>Close</button>
          <button type="button" className="admin-ui-btn admin-ui-btn--primary" onClick={onEdit}>
            <i className="bi bi-pencil"></i> Edit product
          </button>
        </>
      }
    >
      {loading ? (
        <div className="admin-products-detail-loading">
          {[...Array(4)].map((_, i) => <span key={i}></span>)}
        </div>
      ) : product ? (
        <div className="admin-products-detail">
          <div className="admin-products-summary">
            <div className="admin-products-summary-item">
              <span className="admin-products-label">Brand</span>
              <div className="admin-products-brand-info">
                {product.brand?.image && <img src={product.brand.image} alt="" className="admin-products-mini-thumb" />}
                <strong>{product.brand?.name || 'Uncategorized'}</strong>
              </div>
            </div>
            <div className="admin-products-summary-item">
              <span className="admin-products-label">Status</span>
              <StatusPill status={product.status} />
            </div>
            <div className="admin-products-summary-item">
              <span className="admin-products-label">Variants</span>
              <strong>{product.variants?.length || 0}</strong>
            </div>
            <div className="admin-products-summary-item">
              <span className="admin-products-label">Price</span>
              <strong>{priceRange(product.variants)}</strong>
            </div>
            <div className="admin-products-summary-item">
              <span className="admin-products-label">Stock</span>
              <StockPill stock={totalStock(product.variants)} />
            </div>
          </div>

          {product.description && (
            <section className="admin-products-section">
              <h4>Description</h4>
              <p className="admin-products-text">{product.description}</p>
            </section>
          )}

          {product.brand && (
            <section className="admin-products-section">
              <h4>Brand details</h4>
              <dl className="admin-products-dl">
                <dt>Brand name</dt>
                <dd>{product.brand.name}</dd>
                {product.brand.description && (
                  <>
                    <dt>Description</dt>
                    <dd>{product.brand.description}</dd>
                  </>
                )}
              </dl>
            </section>
          )}

          {product.categories?.length > 0 && (
            <section className="admin-products-section">
              <h4>Categories</h4>
              <div className="admin-products-category-list">
                {product.categories.map(cat => (
                  <div key={cat.id || cat.name} className="admin-products-category">
                    {cat.image
                      ? <img src={cat.image} alt="" className="admin-products-mini-thumb" />
                      : <span className="admin-products-mini-thumb admin-products-mini-thumb--empty"><i className="bi bi-tag"></i></span>}
                    <div>
                      <strong>{cat.name}</strong>
                      {cat.description && <span className="admin-ui-muted">{cat.description}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {product.vendor && (
            <section className="admin-products-section">
              <h4>Vendor</h4>
              <p className="admin-products-text">{product.vendor.name || 'No vendor assigned'}</p>
            </section>
          )}

          <section className="admin-products-section">
            <h4>Variant details</h4>
            {product.variants?.length > 0 ? (
              <div className="admin-products-variants">
                <table className="admin-ui-table admin-products-variants-table">
                  <thead>
                    <tr>
                      <th>Variant</th>
                      <th>SKU</th>
                      <th>Stock</th>
                      <th>Unit price</th>
                      <th>Images</th>
                    </tr>
                  </thead>
                  <tbody>
                    {product.variants.map(v => (
                      <tr key={v.id}>
                        <td><strong>{v.name}</strong></td>
                        <td className="admin-ui-mono admin-products-small">{v.sku || 'N/A'}</td>
                        <td>
                          <span className={`admin-products-stock${v.stock > 0 ? '' : ' is-out'}`}>{v.stock} pcs</span>
                        </td>
                        <td>{formatCurrency(v.price)}</td>
                        <td>
                          {v.images?.length > 0 ? (
                            <div className="admin-products-variant-images">
                              {v.images.slice(0, 2).map((img, idx) => (
                                <img key={idx} src={imageUrl(img)} alt={`${v.name}-${idx}`} className="admin-products-mini-thumb" />
                              ))}
                              {v.images.length > 2 && <span className="admin-ui-muted admin-products-small">+{v.images.length - 2}</span>}
                            </div>
                          ) : <span className="admin-ui-muted admin-products-small">No images</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : <p className="admin-ui-muted admin-products-small">No variants found</p>}
          </section>
        </div>
      ) : (
        <EmptyState icon="bi-exclamation-circle" title="Failed to load product details" />
      )}
    </Modal>
  );
}
