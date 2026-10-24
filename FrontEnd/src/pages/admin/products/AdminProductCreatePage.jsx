// src/pages/admin/products/AdminProductCreatePage.jsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { brandService } from '../../../services/brandService';
import { categoryService } from '../../../services/categoryService';
import { productService } from '../../../services/productService';
import ProductForm from '../../../components/product/form/ProductForm';
import { buildProductFormData } from '../../../components/product/form/productFormData';
import { PageHeader, EmptyState } from '../shared/AdminUi';
import './AdminProductFormPage.css';

const API_URL = import.meta.env.VITE_API_URL;

export default function AdminProductCreatePage() {
  useEffect(() => {
    document.title = 'Admin - Create Product';
  }, []);

  const navigate = useNavigate();
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [statuses, setStatuses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  // Fetch brands, categories, statuses
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [brandRes, catRes, statusRes] = await Promise.all([
          brandService.getAll(),
          categoryService.getAll(),
          productService.getStatuses(),
        ]);

        setBrands(brandRes.data?.data || []);
        setCategories(catRes.data?.data || []);
        setStatuses(statusRes.data?.data || []);
      } catch (err) {
        console.error('Failed to load data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);


  const handleSubmit = async (formData) => {
    setSubmitting(true);
    try {
      const res = await fetch(`${API_URL}/api/products`, {
        method: 'POST',
        credentials: 'include',
        body: buildProductFormData(formData),
      });

      const data = await res.json();

      if (res.ok && data.success) {
        window.showSuccess('Product created successfully');
        navigate('/admin/products');
      } else {
        // Return the full error response with message and errors
        return {
          message: data.message || 'Failed to create product',
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
      title="New product"
      description="Fill in the basics, define options, then add variants with price, stock and images."
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

  return (
    <div className="admin-ui-page">
      {header}

      <div className="admin-ui-card admin-product-form-card">
        <div className="admin-product-form-scope">
          <ProductForm
            brands={brands}
            categories={categories}
            statuses={statuses}
            onSubmit={handleSubmit}
            onCancel={handleCancel}
            loading={submitting}
          />
        </div>
      </div>
    </div>
  );
}
