// /my-vendor/products/new and /my-vendor/products/:id/edit — reuses the admin ProductForm (without the
// vendor picker: products always belong to the current store) inside the admin form scope styling.
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { brandService } from '../../../services/brandService';
import { categoryService } from '../../../services/categoryService';
import { productService } from '../../../services/productService';
import { request } from '../../../services/apiService';
import ProductForm from '../../../components/product/form/ProductForm';
import { buildProductFormData, productSaveError } from '../../../components/product/form/productFormData';
import { EmptyState, PageHeader } from '../../admin/shared/AdminUi';
import { canSell, useVendor } from './vendorUtils';
import '../../admin/products/AdminProductFormPage.css';

export default function VendorProductFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { vendor } = useVendor();

  const [state, setState] = useState({ loading: true, product: null, brands: [], categories: [], statuses: [], notFound: false });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    document.title = `My Store - ${isEdit ? 'Edit' : 'New'} product`;
    let cancelled = false;
    (async () => {
      const [brandRes, categoryRes, statusRes, productRes] = await Promise.all([
        brandService.getAll(),
        categoryService.getAll(),
        productService.getStatuses(),
        isEdit ? productService.getById(id) : Promise.resolve(null),
      ]).catch(() => []);
      if (cancelled) return;

      const product = productRes?.ok ? productRes.data?.data ?? productRes.data : null;
      // The API refuses to save someone else's product anyway; this only avoids showing its form.
      // (A product cached before vendorId was added to the response has none: let the API decide.)
      const foreign = product?.vendorId && product.vendorId !== vendor.id;
      setState({
        loading: false,
        product,
        brands: brandRes?.data?.data || [],
        categories: categoryRes?.data?.data || [],
        statuses: statusRes?.data?.data || [],
        notFound: isEdit && (!product || foreign),
      });
    })();
    return () => { cancelled = true; };
  }, [id, isEdit, vendor.id]);

  const handleSubmit = async (formData) => {
    setSubmitting(true);
    try {
      const body = buildProductFormData({ ...formData, vendorId: '' }, { keepExistingImages: isEdit });
      const res = isEdit
        ? await request(`/api/products/${id}`, { method: 'PUT', body })
        : await request('/api/products', { method: 'POST', body });
      if (res.ok) {
        window.showSuccess(isEdit ? 'Product updated' : 'Product created');
        navigate('/my-vendor/products');
        return undefined;
      }
      if (res.status === 403) {
        return productSaveError(null, isEdit ? 'You can only edit products of your own store' : 'Your store must be approved before adding products');
      }
      return productSaveError(res.data, isEdit ? 'Failed to update the product' : 'Failed to create the product');
    } catch {
      window.showError('Server connection error');
      return undefined;
    } finally {
      setSubmitting(false);
    }
  };

  const back = (
    <Link to="/my-vendor/products" className="admin-ui-btn admin-ui-btn--ghost">
      <i className="bi bi-arrow-left"></i> Back to products
    </Link>
  );

  const header = (
    <PageHeader
      eyebrow="Products"
      eyebrowIcon="bi-box-seam"
      title={isEdit ? (state.product?.name || 'Edit product') : 'New product'}
      description="Fill in the basics, define options (size, color…), then add variants with price, stock and images."
      actions={back}
    />
  );

  if (!isEdit && !canSell(vendor)) {
    return (
      <div className="vendor-page">
        {header}
        <div className="admin-ui-card">
          <EmptyState icon="bi-hourglass-split" title="Your store is not approved yet" text="You can add products as soon as an admin activates your store." />
        </div>
      </div>
    );
  }

  if (state.loading || state.notFound) {
    return (
      <div className="vendor-page">
        {header}
        <div className="admin-ui-card">
          {state.loading
            ? <EmptyState icon="bi-hourglass-split" title="Loading…" />
            : <EmptyState icon="bi-search" title="Product not found" text="It does not exist or does not belong to your store." />}
        </div>
      </div>
    );
  }

  return (
    <div className="vendor-page">
      {header}
      <div className="admin-ui-card admin-product-form-card">
        <div className="admin-product-form-scope">
          <ProductForm
            key={state.product?.id || 'new'}
            product={state.product}
            brands={state.brands}
            categories={state.categories}
            statuses={state.statuses}
            onSubmit={handleSubmit}
            onCancel={() => navigate('/my-vendor/products')}
            loading={submitting}
            showVendor={false}
          />
        </div>
      </div>
    </div>
  );
}
