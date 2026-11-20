// src/pages/brand/BrandDetailPage.jsx
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { brandService } from '../../services/brandService';
import { productService } from '../../services/productService';
import ProductCard from '../../components/product/card/ProductCard';
import defaultBrandImage from '../../assets/product.png';
import './BrandDetailPage.css';

export default function BrandDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  
  const [brand, setBrand] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pagination, setPagination] = useState({
    currentPage: 0,
    pageSize: 10,
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [brandRes, prodRes] = await Promise.all([
          brandService.getById(id),
          productService.getAll({ brandId: id, page: 0, size: pagination.pageSize }),
        ]);
        
        if (brandRes.ok && brandRes.data && brandRes.data.data) {
          setBrand(brandRes.data.data);
        } else {
          setError('Brand not found');
        }
        
        if (prodRes.ok && prodRes.data) {
          setProducts(prodRes.data.data || []);
          setPagination(prodRes.data.pagination || pagination);
        }
      } catch {
        setError('Failed to load data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  const handlePageChange = async (newPage) => {
    try {
      setLoading(true);
      const params = {
        brandId: id,
        page: newPage,
        size: pagination.pageSize,
      };
      const res = await productService.getAll(params);
      if (res.ok && res.data) {
        setProducts(res.data.data || []);
        setPagination(res.data.pagination || pagination);
      }
    } catch {
      setError('Failed to load products');
    } finally {
      setLoading(false);
    }
  };

  const handleCartUpdate = () => {
    // Refresh cart if needed
  };

  if (loading) {
    return (
      <div className="brand-detail-page">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading...</p>
        </div>
      </div>
    );
  }

  if (error || !brand) {
    return (
      <div className="brand-detail-page">
        <div className="error-state">
          <p>{error || 'Brand not found'}</p>
          <button className="btn-back" onClick={() => navigate('/home')}>
            Back to Home
          </button>
        </div>
      </div>
    );
  }

  const brandImageUrl = brand.image || defaultBrandImage;

  return (
    <div className="brand-detail-page">
      <button className="btn-back" onClick={() => navigate(-1)}>
        ← Back
      </button>

      <div className="brand-header">
        <div className="brand-logo-section">
          <img
            src={brandImageUrl}
            alt={brand.name}
            className="brand-logo"
            onError={(e) => { e.target.src = defaultBrandImage; }}
          />
        </div>
        <div className="brand-info-section">
          <h1 className="brand-title">{brand.name}</h1>
          {brand.description && (
            <p className="brand-description">{brand.description}</p>
          )}
          <p className="product-count">
            {pagination.totalElements} {pagination.totalElements === 1 ? 'product' : 'products'}
          </p>
        </div>
      </div>

      <div className="brand-products-section">
        <h2 className="section-title">Products</h2>
        
        {products.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📦</div>
            <h3>No products found</h3>
            <p>This brand doesn't have any products yet.</p>
          </div>
        ) : (
          <>
            <div className="brand-detail-product-grid">
              {products.map(product => (
                <ProductCard 
                  key={product.id} 
                  product={product} 
                  onCartUpdate={handleCartUpdate}
                />
              ))}
            </div>
            
            {pagination.totalPages > 1 && (
              <div className="brand-details-pagination-controls">
                <button
                  className="brand-details-pagination-btn"
                  disabled={!pagination.hasPrevious}
                  onClick={() => handlePageChange(pagination.currentPage - 1)}
                >
                  Previous
                </button>
                
                <div className="brand-details-pagination-numbers">
                  {Array.from({ length: pagination.totalPages }, (_, i) => (
                    <button
                      key={i}
                      className={`pagination-number ${pagination.currentPage === i ? 'active' : ''}`}
                      onClick={() => handlePageChange(i)}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
                
                <button
                  className="brand-details-pagination-btn"
                  disabled={!pagination.hasNext}
                  onClick={() => handlePageChange(pagination.currentPage + 1)}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
