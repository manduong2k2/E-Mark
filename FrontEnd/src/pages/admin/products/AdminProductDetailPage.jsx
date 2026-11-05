// src/pages/admin/products/AdminProductDetailPage.jsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productService } from '../../../services/productService';
import { brandService } from '../../../services/brandService';
import { categoryService } from '../../../services/categoryService';
import { productService as productApi } from '../../../services/productService';
import defaultProductImage from '../../../assets/product.png';
import { PageHeader, Pill, EmptyState, ConfirmDialog } from '../shared/AdminUi';
import { formatCurrency, formatDateTime } from '../shared/adminUiUtils';
import './AdminProductDetailPage.css';

const STATUS_TONES = {
  PUBLISHED: 'success',
  ACTIVE: 'success',
  DRAFT: 'neutral',
  PENDING: 'warning',
  PENDING_APPROVAL: 'warning',
  OUT_OF_STOCK: 'warning',
  ARCHIVED: 'danger',
  HIDDEN: 'danger',
  INACTIVE: 'danger',
  REJECTED: 'danger',
  BANNED: 'danger',
};

const humanize = (value) => String(value)
  .split('_')
  .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
  .join(' ');

function StatusPill({ status }) {
  if (!status) return <Pill tone="neutral">Unknown</Pill>;
  return <Pill tone={STATUS_TONES[status] || 'neutral'}>{humanize(status)}</Pill>;
}

function StockPill({ stock }) {
  const value = Number(stock);
  if (stock === null || stock === undefined || Number.isNaN(value)) return <span className="admin-ui-muted">—</span>;
  if (value <= 0) return <Pill tone="danger">Out of stock</Pill>;
  if (value <= 5) return <Pill tone="warning">{value} left</Pill>;
  return <Pill tone="success">{value} in stock</Pill>;
}

export default function AdminProductDetailPage() {
  useEffect(() => { document.title = 'Admin - Product Detail'; }, []);

  const { id } = useParams();
  const navigate = useNavigate();

  const [product, setProduct] = useState(null);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeImage, setActiveImage] = useState(0);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [prodRes, brandRes, catRes] = await Promise.all([
          productService.getById(id),
          brandService.getAll(),
          categoryService.getAll(),
        ]);

        if (prodRes.ok && prodRes.data) {
          // Handle both response formats: { success, data: {...} } or direct {...}
          setProduct(prodRes.data.data || prodRes.data);
        } else {
          setError('Product not found');
        }

        setBrands(brandRes.data?.data || []);
        setCategories(catRes.data?.data || []);
      } catch {
        setError('Failed to load data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  const getBrandName = (product) => {
    if (product.brand?.name) return product.brand.name;
    const brand = brands.find((b) => b.id === product.brandId);
    return brand ? brand.name : null;
  };

  const getCategoryNames = (product) => {
    if (product.categories && product.categories.length > 0) {
      return product.categories.map(cat => cat.name);
    }
    if (product.categoryIds && product.categoryIds.length > 0) {
      return product.categoryIds
        .map(catId => categories.find(c => c.id === catId)?.name)
        .filter(Boolean);
    }
    return [];
  };

  const handleEdit = () => {
    navigate(`/admin/products/edit/${id}`);
  };

  const handleDelete = async () => {
    setDeleting(true);
    try {
      const res = await productApi.delete(id);
      if (res.ok) {
        window.showSuccess('Product deleted successfully');
        navigate('/admin/products');
      } else {
        window.showError(res.data?.message || 'Failed to delete product');
      }
    } catch {
      window.showError('Server connection error');
    } finally {
      setDeleting(false);
      setConfirmingDelete(false);
    }
  };

  const backButton = (
    <button type="button" className="admin-ui-btn admin-ui-btn--ghost" onClick={() => navigate('/admin/products')}>
      <i className="bi bi-arrow-left"></i> Back to list
    </button>
  );

  if (loading) {
    return (
      <div className="admin-ui-page">
        <PageHeader eyebrow="Catalog" eyebrowIcon="bi-box-seam" title="Product detail" actions={backButton} />
        <div className="admin-ui-card">
          <EmptyState icon="bi-hourglass-split" title="Loading product…" />
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="admin-ui-page">
        <PageHeader eyebrow="Catalog" eyebrowIcon="bi-box-seam" title="Product detail" actions={backButton} />
        <div className="admin-ui-card">
          <EmptyState icon="bi-exclamation-octagon" title={error || 'Product not found'} text="The product may have been deleted or the server is unreachable.">
            <button type="button" className="admin-ui-btn admin-ui-btn--ghost admin-product-detail-empty-action" onClick={() => navigate('/admin/products')}>
              Back to Products
            </button>
          </EmptyState>
        </div>
      </div>
    );
  }

  const variants = product.variants || [];
  const brandName = getBrandName(product);
  const categoryNames = getCategoryNames(product);

  // Product-level images if present, otherwise all variant images (deduplicated)
  const images = product.images && product.images.length > 0
    ? product.images
    : [...new Set(variants.flatMap(v => v.images || []))];
  const heroImage = images[activeImage]?.url || images[0]?.url || defaultProductImage;

  const totalStock = variants.reduce((sum, v) => sum + (Number(v.stock) || 0), 0);
  const prices = variants.map(v => Number(v.price)).filter(p => !Number.isNaN(p));
  const minPrice = prices.length ? Math.min(...prices) : null;
  const maxPrice = prices.length ? Math.max(...prices) : null;

  const hasLegacyPrice = product.price !== undefined && product.price !== null;
  const hasLegacyStock = product.stock !== undefined && product.stock !== null;

  const meta = [
    brandName,
    `${variants.length} variant${variants.length === 1 ? '' : 's'}`,
    product.vendor?.name && `sold by ${product.vendor.name}`,
  ].filter(Boolean).join(' · ');

  return (
    <div className="admin-ui-page">
      <PageHeader
        eyebrow="Catalog"
        eyebrowIcon="bi-box-seam"
        title={product.name || 'Untitled product'}
        description={meta}
        actions={
          <>
            {backButton}
            <button type="button" className="admin-ui-btn admin-ui-btn--ghost admin-ui-btn--danger-text" onClick={() => setConfirmingDelete(true)}>
              <i className="bi bi-trash3"></i> Delete
            </button>
            <button type="button" className="admin-ui-btn admin-ui-btn--primary" onClick={handleEdit}>
              <i className="bi bi-pencil"></i> Edit
            </button>
          </>
        }
      />

      {/* Hero */}
      <section className="admin-ui-card admin-product-detail-hero">
        <div className="admin-product-detail-media">
          <div className="admin-product-detail-cover">
            <img src={heroImage} alt={product.name} />
            {images.length > 1 && (
              <span className="admin-product-detail-cover-count">
                <i className="bi bi-images"></i> {Math.min(activeImage, images.length - 1) + 1}/{images.length}
              </span>
            )}
          </div>
          {images.length > 1 && (
            <div className="admin-product-detail-thumbs">
              {images.map((img, idx) => (
                <button
                  key={img + idx}
                  type="button"
                  className={`admin-product-detail-thumb${idx === activeImage ? ' is-active' : ''}`}
                  onClick={() => setActiveImage(idx)}
                  aria-label={`Show image ${idx + 1}`}
                >
                  <img src={img} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="admin-product-detail-info">
          <div className="admin-product-detail-title-row">
            <h2>{product.name || 'Untitled product'}</h2>
            <StatusPill status={product.status} />
          </div>

          <dl className="admin-product-detail-facts">
            <div>
              <dt>Brand</dt>
              <dd className="admin-product-detail-brand">
                {product.brand?.image && <img src={product.brand.image} alt="" />}
                {brandName || <span className="admin-ui-muted">—</span>}
              </dd>
            </div>
            <div>
              <dt>Price</dt>
              <dd className="admin-product-detail-price">
                {hasLegacyPrice
                  ? formatCurrency(product.price)
                  : minPrice === null
                    ? <span className="admin-ui-muted">—</span>
                    : minPrice === maxPrice
                      ? formatCurrency(minPrice)
                      : `${formatCurrency(minPrice)} – ${formatCurrency(maxPrice)}`}
              </dd>
            </div>
            <div>
              <dt>Stock</dt>
              <dd><StockPill stock={hasLegacyStock ? product.stock : (variants.length ? totalStock : null)} /></dd>
            </div>
            {product.code && (
              <div>
                <dt>Code</dt>
                <dd><code className="admin-product-detail-code">{product.code}</code></dd>
              </div>
            )}
          </dl>

          <div className="admin-product-detail-block">
            <span className="admin-product-detail-label">Categories</span>
            {categoryNames.length > 0 ? (
              <div className="admin-ui-chips">
                {categoryNames.map(name => (
                  <span key={name} className="admin-ui-chip"><i className="bi bi-tag"></i> {name}</span>
                ))}
              </div>
            ) : (
              <span className="admin-ui-muted">—</span>
            )}
          </div>

          <div className="admin-product-detail-block">
            <span className="admin-product-detail-label">Description</span>
            <p className="admin-product-detail-description">
              {product.description || <span className="admin-ui-muted">No description.</span>}
            </p>
          </div>
        </div>
      </section>

      {/* Variants */}
      <section className="admin-ui-card admin-product-detail-section">
        <header className="admin-product-detail-section-head">
          <h3><i className="bi bi-layers"></i> Variants</h3>
          <span className="admin-ui-tag">{variants.length}</span>
        </header>
        {variants.length > 0 ? (
          <div className="admin-ui-table-wrap">
            <table className="admin-ui-table admin-product-detail-table">
              <thead>
                <tr>
                  <th>Variant</th>
                  <th>Price</th>
                  <th>Stock</th>
                  <th>Images</th>
                </tr>
              </thead>
              <tbody>
                {variants.map((v, idx) => (
                  <tr key={v.id || idx}>
                    <td>
                      <div className="admin-product-detail-variant">
                        <strong>{v.name || `Variant ${idx + 1}`}</strong>
                        <span className="admin-ui-mono">{v.sku || 'No SKU'}</span>
                      </div>
                    </td>
                    <td className="admin-product-detail-money">{formatCurrency(v.price)}</td>
                    <td><StockPill stock={v.stock} /></td>
                    <td>
                      {v.images && v.images.length > 0 ? (
                        <div className="admin-product-detail-mini-thumbs">
                          {[...v.images].slice(0, 3).map((img, iIdx) => (
                            <img key={iIdx} src={img} alt={`${v.name || 'variant'}-${iIdx}`} />
                          ))}
                          {v.images.length > 3 && <span>+{v.images.length - 3}</span>}
                        </div>
                      ) : (
                        <span className="admin-ui-muted">No images</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon="bi-layers" title="No variants" text="This product has no variants yet." />
        )}
      </section>

      <div className="admin-product-detail-grid">
        {/* Record details */}
        <section className="admin-ui-card admin-product-detail-section">
          <header className="admin-product-detail-section-head">
            <h3><i className="bi bi-info-circle"></i> Details</h3>
          </header>
          <dl className="admin-product-detail-meta">
            <div>
              <dt>ID</dt>
              <dd><code className="admin-product-detail-code">{product.id}</code></dd>
            </div>
            {product.vendor && (
              <div>
                <dt>Vendor</dt>
                <dd>{product.vendor.name || 'No vendor assigned'}</dd>
              </div>
            )}
            <div>
              <dt>Created</dt>
              <dd>{formatDateTime(product.createdAt)}</dd>
            </div>
            <div>
              <dt>Updated</dt>
              <dd>{formatDateTime(product.updatedAt)}</dd>
            </div>
          </dl>
        </section>
      </div>

      {/* Images */}
      <section className="admin-ui-card admin-product-detail-section">
        <header className="admin-product-detail-section-head">
          <h3><i className="bi bi-images"></i> Images</h3>
          <span className="admin-ui-tag">{images.length}</span>
        </header>
        {images.length > 0 ? (
          <div className="admin-product-detail-gallery">
            {images.map((img, idx) => (
              <button
                key={img + idx}
                type="button"
                className={`admin-product-detail-gallery-item${idx === activeImage ? ' is-active' : ''}`}
                onClick={() => setActiveImage(idx)}
                aria-label={`Show image ${idx + 1} in the cover`}
              >
                <img src={img} alt={`${product.name} ${idx + 1}`} />
              </button>
            ))}
          </div>
        ) : (
          <EmptyState icon="bi-image" title="No images" text="Images are uploaded per variant from the edit page." />
        )}
      </section>

      {confirmingDelete && (
        <ConfirmDialog
          title="Delete product"
          message={<>Are you sure you want to delete <strong>{product.name}</strong>? This cannot be undone.</>}
          busy={deleting}
          onConfirm={handleDelete}
          onClose={() => setConfirmingDelete(false)}
        />
      )}
    </div>
  );
}
