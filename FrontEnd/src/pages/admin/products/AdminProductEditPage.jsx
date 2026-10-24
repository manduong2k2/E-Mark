// src/pages/admin/products/AdminProductEditPage.jsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productService } from '../../../services/productService';
import { brandService } from '../../../services/brandService';
import { categoryService } from '../../../services/categoryService';
import ProductForm from '../../../components/product/form/ProductForm';
import { buildProductFormData } from '../../../components/product/form/productFormData';
import { PageHeader, EmptyState } from '../shared/AdminUi';
import './AdminProductFormPage.css';

const API_URL = import.meta.env.VITE_API_URL;

export default function AdminProductEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    document.title = 'Admin - Edit Product';
  }, []);

  const [product, setProduct] = useState(null);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Fetch product detail and metadata
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [prodRes, brandRes, catRes, statusRes] = await Promise.all([
          productService.getById(id),
          brandService.getAll(),
          categoryService.getAll(),
          productService.getStatuses(),
        ]);

        if (!prodRes.ok || !prodRes.data) {
          setError('Product not found');
          return;
        }

        // Extract actual product data from nested response
        const productData = prodRes.data.data || prodRes.data;
        setProduct(productData);
        setBrands(brandRes.data?.data || []);
        setCategories(catRes.data?.data || []);
        setStatuses(statusRes.data?.data || []);
      } catch (err) {
        setError('Failed to load data');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);


  const handleSubmit = async (formData) => {
    setSubmitting(true);
    try {
      const res = await fetch(`${API_URL}/api/products/${id}`, {
        method: 'PUT',
        credentials: 'include',
        body: buildProductFormData(formData, { keepExistingImages: true }),
      });

      const data = await res.json();

      if (res.ok && data.success) {
        window.showSuccess('Product updated successfully');
        navigate('/admin/products');
      } else {
        // Return the full error response with message and errors
        return {
          message: data.message || 'Failed to update product',
          errors: data.errors || {}
        };
      }
    } catch (err) {
      window.showError('Server connection error');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = () => {
    navigate('/admin/products');
  };

  const header = (
    <PageHeader
      eyebrow="Catalog"
      eyebrowIcon="bi-box-seam"
      title="Edit product"
      description={product?.name ? `Editing “${product.name}”. Changes are saved when you submit the form.` : 'Update product details, options and variants.'}
      actions={
        <button type="button" className="admin-ui-btn admin-ui-btn--ghost" onClick={handleCancel}>
          <i className="bi bi-arrow-left"></i> Back to list
        </button>
      }
    />
  );

  if (loading) {
    return (
      <div className="admin-ui-page">
        {header}
        <div className="admin-ui-card">
          <EmptyState icon="bi-hourglass-split" title="Loading…" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="admin-ui-page">
        {header}
        <div className="admin-ui-card">
          <EmptyState icon="bi-exclamation-octagon" title={error}>
            <button
              type="button"
              className="admin-ui-btn admin-ui-btn--ghost admin-product-form-empty-action"
              onClick={() => navigate('/admin/products')}
            >
              Back to Products
            </button>
          </EmptyState>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-ui-page">
      {header}

      {product && (
        <div className="admin-ui-card admin-product-form-card">
          <div className="admin-product-form-scope">
            <ProductForm
              product={product}
              brands={brands}
              categories={categories}
              statuses={statuses}
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              loading={submitting}
            />
          </div>
        </div>
      )}
    </div>
  );
}
