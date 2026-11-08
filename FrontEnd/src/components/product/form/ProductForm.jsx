import { useState, useRef, useId } from 'react';
import CategoryTreeSelect from '../../category/tree/CategoryTreeSelect';
import SearchableVendorSelect from '../../vendor/select/SearchableVendorSelect';
import ProductOptionsEditor from './ProductOptionsEditor';
import { retainOptionSelections, selectOptionValue, validateProductOptions } from './productOptions';
import './ProductForm.css';

const FORM_STEPS = [
  { id: 'basic', label: 'Basic Information' },
  { id: 'options', label: 'Product Options' },
  { id: 'variants', label: 'Product Variants' },
];

function errorStep(field) {
  if (field.startsWith('attributes')) return 'options';
  if (field.startsWith('variant_') || field.startsWith('variants')) return 'variants';
  return 'basic';
}

export default function ProductForm(props) {
  return <ProductFormFields key={props.product?.id || 'new'} {...props} />;
}

function ProductFormFields({
  product,
  brands = [],
  categories = [],
  statuses = [],
  onSubmit,
  onCancel,
  loading = false,
  // false on vendor pages: a vendor always creates products for its own store
  showVendor = true,
}) {
  const defaultStatus = statuses.length > 0 ? statuses[0] : '';

  const [formData, setFormData] = useState(() => ({
    name: product?.name || '',
    description: product?.description || '',
    brandId: product?.brandId || product?.brand?.id || '',
    vendorId: product?.vendorId || product?.vendor?.id || '',
    categoryIds: product?.categoryIds || product?.categories?.map((c) => c.id) || [],
    status: product?.status || defaultStatus,
    attributes: [],
    variants: product?.variants?.length ? product.variants.map((v) => ({
      ...v,
      tempId: v.id || v.tempId || Date.now() + Math.random(),
      name: v.name || '',
      price: v.price ?? '',
      stock: v.stock ?? '',
      sku: v.sku || '',
      images: Array.isArray(v.images) ? v.images.map(image => ({
        type: 'existing',
        id: typeof image === 'string' ? null : image.id,
        url: typeof image === 'string' ? image : image.url,
      })) : [],
      attributeValueIds: [],
    })) : [
        {
          tempId: Date.now() + Math.random(),
          name: '',
          price: '',
          stock: '',
          sku: '',
          images: [],
          attributeValueIds: [],
        },
      ],
  }));

  const [errors, setErrors] = useState({});
  const [activeStep, setActiveStep] = useState('basic');
  const formId = useId();
  const tabRefs = useRef({});
  const variantFileInputRefs = useRef({});
  const activeStepIndex = FORM_STEPS.findIndex(step => step.id === activeStep);
  const hasStepErrors = step => Object.entries(errors).some(([field, message]) => message && errorStep(field) === step);

  const changeStep = (step, focusTab = false) => {
    setActiveStep(step);
    if (focusTab) tabRefs.current[step]?.focus();
  };

  const handleTabKeyDown = (event, index) => {
    let nextIndex;
    if (event.key === 'ArrowRight') nextIndex = (index + 1) % FORM_STEPS.length;
    else if (event.key === 'ArrowLeft') nextIndex = (index + FORM_STEPS.length - 1) % FORM_STEPS.length;
    else if (event.key === 'Home') nextIndex = 0;
    else if (event.key === 'End') nextIndex = FORM_STEPS.length - 1;
    else return;
    event.preventDefault();
    changeStep(FORM_STEPS[nextIndex].id, true);
  };

  const showValidationErrors = (fieldErrors) => {
    setErrors(fieldErrors);
    const firstStep = FORM_STEPS.find(step => Object.entries(fieldErrors).some(([field, message]) =>
      message && errorStep(field) === step.id));
    if (firstStep) changeStep(firstStep.id, true);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const handleAddVariantImages = (variantIdx, e) => {
    const files = Array.from(e.target.files);
    e.target.value = '';

    files.forEach((file) => {
      if (!file.type.startsWith('image/')) return;
      if (file.size > 5 * 1024 * 1024) return;

      const reader = new FileReader();
      reader.onloadend = () => {
        setFormData((prev) => {
          const updatedVariants = [...prev.variants];
          updatedVariants[variantIdx] = {
            ...updatedVariants[variantIdx],
            images: [
              ...updatedVariants[variantIdx].images,
              { type: 'new', file, preview: reader.result },
            ],
          };
          return { ...prev, variants: updatedVariants };
        });
      };
      reader.readAsDataURL(file);
    });
  };

  const handleOptionsChange = (attributes) => {
    setFormData(prev => ({
      ...prev,
      attributes,
      variants: retainOptionSelections(prev.variants, attributes),
    }));
    setErrors(prev => Object.fromEntries(Object.entries(prev).filter(([key]) =>
      !key.startsWith('attributes') && !key.endsWith('_attributeValueIds'))));
  };

  const handleRemoveVariantImage = (variantIdx, imageIdx) => {
    setFormData((prev) => {
      const updatedVariants = [...prev.variants];
      updatedVariants[variantIdx] = {
        ...updatedVariants[variantIdx],
        images: updatedVariants[variantIdx].images.filter((_, i) => i !== imageIdx),
      };
      return { ...prev, variants: updatedVariants };
    });
  };

  const validate = () => {
    const newErrors = product ? {} : validateProductOptions(formData.attributes, formData.variants);
    if (!formData.name.trim()) newErrors.name = 'Product name is required';
    if (!formData.brandId) newErrors.brandId = 'Please select a brand';

    if (!formData.variants || formData.variants.length === 0) {
      newErrors.variants = 'At least one variant is required';
    } else {
      const skus = new Set();
      formData.variants.forEach((v, idx) => {
        if (!v.name?.trim()) newErrors[`variant_${idx}_name`] = 'Variant name is required';
        if (v.price === '' || isNaN(Number(v.price)) || Number(v.price) < 0)
          newErrors[`variant_${idx}_price`] = 'Invalid price (must be ≥ 0)';
        if (v.stock === '' || isNaN(Number(v.stock)) || Number(v.stock) < 0)
          newErrors[`variant_${idx}_stock`] = 'Invalid stock (must be ≥ 0)';
        if (!v.sku?.trim()) newErrors[`variant_${idx}_sku`] = 'SKU is required';
        else if (skus.has(v.sku.trim())) newErrors[`variant_${idx}_sku`] = 'Each variant SKU must be unique';
        skus.add(v.sku?.trim());
      });
    }

    showValidationErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    const serverErrors = await onSubmit({
      ...formData,
      attributes: formData.attributes.map(attribute => ({
        ...attribute,
        code: attribute.code.trim(),
        name: attribute.name.trim(),
        description: attribute.description.trim(),
        values: attribute.values.map(value => ({ ...value, value: value.value.trim() })),
      })),
      variants: formData.variants,
    });

    // Handle server errors
    if (serverErrors && typeof serverErrors === 'object') {
      // Check if response has message and errors structure
      if (serverErrors.message && serverErrors.errors) {
        // Show message in popup
        window.showError(serverErrors.message, 'Validation Error');
        // Convert server error keys to client format
        const mappedErrors = {};
        Object.entries(serverErrors.errors).forEach(([key, value]) => {
          // Convert variants[0].sku -> variant_0_sku
          let mappedKey = key;
          mappedKey = mappedKey.replace(/variants\[(\d+)\]\./, 'variant_$1_');
          mappedKey = mappedKey.replace(/(attributeValueIds)\[\d+\]$/, '$1');
          mappedErrors[mappedKey] = value;
        });
        // Bind field errors
        showValidationErrors(mappedErrors);
      } else if (serverErrors._ || serverErrors.message) {
        // Legacy format or simple message
        const errorMessage = serverErrors._ || serverErrors.message;
        window.showError(errorMessage, 'Validation Error');
        const { _: _ignored, message: _msgIgnored, ...fieldErrors } = serverErrors;
        if (Object.keys(fieldErrors).length > 0) {
          showValidationErrors(fieldErrors);
        }
      } else {
        // Field errors only
        showValidationErrors(serverErrors);
      }
    }
  };

  const handleAddVariant = () => {
    const newVariant = {
      tempId: Date.now() + Math.random(),
      name: '',
      price: '',
      stock: '',
      sku: '',
      images: [],
      attributeValueIds: [],
    };
    setFormData((prev) => ({
      ...prev,
      variants: [...prev.variants, newVariant],
    }));
  };

  const handleRemoveVariant = (index) => {
    if (formData.variants.length > 1) {
      setFormData((prev) => ({
        ...prev,
        variants: prev.variants.filter((_, i) => i !== index),
      }));
    }
  };

  const handleVariantChange = (index, field, value) => {
    setFormData((prev) => {
      const updatedVariants = [...prev.variants];
      updatedVariants[index] = { ...updatedVariants[index], [field]: value };
      return { ...prev, variants: updatedVariants };
    });
    if (errors[`variant_${index}_${field}`]) {
      setErrors((prev) => ({ ...prev, [`variant_${index}_${field}`]: '' }));
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.currentTarget.classList.add('drag-over');
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.currentTarget.classList.remove('drag-over');
  };

  const handleDrop = (e, variantIdx) => {
    e.preventDefault();
    e.stopPropagation();
    e.currentTarget.classList.remove('drag-over');
    if (e.dataTransfer && e.dataTransfer.files) {
      handleAddVariantImages(variantIdx, { target: { files: e.dataTransfer.files, value: '' } });
    }
  };

  const getOrdinal = (n) => {
    if (n % 100 >= 11 && n % 100 <= 13) return `${n}th`;

    switch (n % 10) {
      case 1: return `${n}st`;
      case 2: return `${n}nd`;
      case 3: return `${n}rd`;
      default: return `${n}th`;
    }
  };

  return (
    <div className="product-form-wrapper">
      <div className="form-container">
        <form onSubmit={handleSubmit} className="product-form product-form--steps">
          <div className="product-form-tabs" role="tablist" aria-label="Product form steps">
            {FORM_STEPS.map((step, index) => (
              <button key={step.id} type="button" role="tab"
                ref={element => { tabRefs.current[step.id] = element; }}
                id={`${formId}-tab-${step.id}`} aria-controls={`${formId}-panel-${step.id}`}
                aria-selected={activeStep === step.id} tabIndex={activeStep === step.id ? 0 : -1}
                aria-label={`${index + 1} ${step.label}${hasStepErrors(step.id) ? ', contains errors' : ''}`}
                className={`product-form-tab${activeStep === step.id ? ' is-active' : ''}${hasStepErrors(step.id) ? ' has-errors' : ''}`}
                disabled={loading} onClick={() => changeStep(step.id)}
                onKeyDown={event => handleTabKeyDown(event, index)}>
                <span className="product-form-tab-number" aria-hidden="true">{index + 1}</span>
                <span className="product-form-tab-label">{step.label}</span>
                {hasStepErrors(step.id) && <span className="product-form-tab-error" aria-hidden="true">!</span>}
              </button>
            ))}
          </div>

          <div className="product-form-panel" role="tabpanel" id={`${formId}-panel-basic`}
            aria-labelledby={`${formId}-tab-basic`} hidden={activeStep !== 'basic'}>
            {/* Section 1: Basic Information */}
            <div className="form-section">
              <div className="section-header">
                <div>
                  <h3 className="section-title">
                    <i className="fa-solid fa-circle-info"></i> Basic Information
                  </h3>
                  <p className="section-subtitle">Product name, brand, and category</p>
                </div>
              </div>

              <div className="form-grid form-grid-2col">
                {/* Product Name */}
                <div className="form-group form-group-full">
                  <label className="form-label">
                    Product Name <span className="required">*</span>
                  </label>
                  <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    placeholder="e.g. Laptop Gaming Asus ROG Strix"
                    className={`form-input ${errors.name ? 'error' : ''}`}
                  />
                  {errors.name && <span className="error-message">{errors.name}</span>}
                </div>

                {/* Description */}
                <div className="form-group form-group-full">
                  <label className="form-label">Product Description</label>
                  <textarea
                    name="description"
                    value={formData.description}
                    onChange={handleChange}
                    placeholder="Enter high performance features, display, battery..."
                    rows="3"
                    className="form-textarea"
                  />
                </div>

                {/* Vendor */}
                {showVendor && (
                  <div className="form-group form-group-full">
                    <label className="form-label">Vendor</label>
                    <SearchableVendorSelect
                      value={formData.vendorId}
                      onChange={handleChange}
                      error={errors.vendorId}
                    />
                  </div>
                )}

                {/* Brand */}
                <div className="form-group">
                  <label className="form-label">
                    Brand <span className="required">*</span>
                  </label>
                  <select
                    name="brandId"
                    value={formData.brandId}
                    onChange={handleChange}
                    className={`form-select ${errors.brandId ? 'error' : ''}`}
                  >
                    <option value="">-- Select Brand --</option>
                    {brands.map((b) => (
                      <option key={b.id} value={b.id}>{b.name}</option>
                    ))}
                  </select>
                  {errors.brandId && <span className="error-message">{errors.brandId}</span>}
                </div>

                {/* Status */}
                <div className="form-group">
                  <label className="form-label">Status</label>
                  <select
                    name="status"
                    value={formData.status}
                    onChange={handleChange}
                    className="form-select"
                  >
                    {statuses.map((s) => (
                      <option key={s} value={s}>
                        {s.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Categories */}
              {categories.length > 0 && (
                <div className="form-group">
                  <label className="form-label">
                    Categories
                    {formData.categoryIds.length > 0 && (
                      <span className="cat-selected-summary"> — {formData.categoryIds.length} selected</span>
                    )}
                  </label>
                  <CategoryTreeSelect
                    categories={categories}
                    selectedIds={formData.categoryIds}
                    onChange={(ids) => setFormData((prev) => ({ ...prev, categoryIds: ids }))}
                  />
                </div>
              )}
            </div>
          </div>

          <div className="product-form-panel" role="tabpanel" id={`${formId}-panel-options`}
            aria-labelledby={`${formId}-tab-options`} hidden={activeStep !== 'options'}>
            {!product ? (
              <ProductOptionsEditor attributes={formData.attributes} onChange={handleOptionsChange}
                errors={errors} disabled={loading} />
            ) : (
              <section className="form-section">
                <h3 className="section-title">Product Options</h3>
                <p className="section-subtitle">Options can be configured when creating a product.</p>
              </section>
            )}
          </div>

          <div className="product-form-panel" role="tabpanel" id={`${formId}-panel-variants`}
            aria-labelledby={`${formId}-tab-variants`} hidden={activeStep !== 'variants'}>
            {/* Section 3: Product Variants */}
            <div className="form-section">
              <div className="section-header">
                <div>
                  <h3 className="section-title">
                    <i className="fa-solid fa-cubes"></i> Product Variants
                  </h3>
                  <p className="section-subtitle">Manage stock items and upload multiple variant photos</p>
                </div>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleAddVariant}
                >
                  <i className="fa-solid fa-plus"></i> Add Variant
                </button>
              </div>

              {errors.variants && <span className="error-message">{errors.variants}</span>}

              {formData.variants.length === 0 ? (
                <p className="empty-message">No variants available. Click "+ Add Variant" to create one.</p>
              ) : (
                <div className="variants-list">
                  {formData.variants.map((variant, idx) => (
                    <div key={variant.tempId || idx} className="variant-item">
                      <div className="variant-header">
                        <span className="variant-index">{getOrdinal(idx + 1)} variant</span>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => handleRemoveVariant(idx)}
                          disabled={formData.variants.length === 1}
                          title={formData.variants.length === 1 ? 'At least one variant is required' : ''}
                        >
                          <i className="fa-solid fa-trash-can"></i> Remove
                        </button>
                      </div>

                      {/* Variant Fields */}
                      <div className="form-grid form-grid-2col">
                        <div className="form-group form-group-full">
                          <label className="form-label">Name</label>
                          <input
                            type="text"
                            placeholder="e.g. 16GB RAM / 512GB SSD"
                            value={variant.name}
                            onChange={(e) => handleVariantChange(idx, 'name', e.target.value)}
                            className={`form-input ${errors[`variant_${idx}_name`] ? 'error' : ''}`}
                          />
                          {errors[`variant_${idx}_name`] && (
                            <span className="error-message">{errors[`variant_${idx}_name`]}</span>
                          )}
                        </div>

                        <div className="form-group">
                          <label className="form-label">Price</label>
                          <input
                            type="number"
                            placeholder="0"
                            value={variant.price}
                            onChange={(e) => handleVariantChange(idx, 'price', e.target.value)}
                            min="0"
                            step="0.01"
                            className={`form-input ${errors[`variant_${idx}_price`] ? 'error' : ''}`}
                          />
                          {errors[`variant_${idx}_price`] && (
                            <span className="error-message">{errors[`variant_${idx}_price`]}</span>
                          )}
                        </div>

                        <div className="form-group">
                          <label className="form-label">Stock</label>
                          <input
                            type="number"
                            placeholder="10"
                            value={variant.stock}
                            onChange={(e) => handleVariantChange(idx, 'stock', e.target.value)}
                            min="0"
                            className={`form-input ${errors[`variant_${idx}_stock`] ? 'error' : ''}`}
                          />
                          {errors[`variant_${idx}_stock`] && (
                            <span className="error-message">{errors[`variant_${idx}_stock`]}</span>
                          )}
                        </div>

                        <div className="form-group form-group-full">
                          <label className="form-label">SKU</label>
                          <input
                            type="text"
                            placeholder="SKU Identifier"
                            value={variant.sku}
                            onChange={(e) => handleVariantChange(idx, 'sku', e.target.value)}
                            className={`form-input ${errors[`variant_${idx}_sku`] ? 'error' : ''}`}
                          />
                          {errors[`variant_${idx}_sku`] && (
                            <span className="error-message">{errors[`variant_${idx}_sku`]}</span>
                          )}
                        </div>
                      </div>

                      {!product && formData.attributes.length > 0 && (
                        <fieldset className="product-variant-options" disabled={loading}>
                          <legend>Option selections</legend>
                          <div className="form-grid form-grid-2col">
                            {formData.attributes.map((attribute, attributeIndex) => {
                              const selectId = `variant-${variant.tempId}-option-${attribute.tempId}`;
                              const optionError = errors[`variant_${idx}_attributeValueIds`];
                              const selectedValue = attribute.values.find(value =>
                                (variant.attributeValueIds || []).includes(value.tempId));
                              return (
                                <div className="form-group" key={attribute.tempId}>
                                  <label className="form-label" htmlFor={selectId}>
                                    {attribute.name.trim() || `Option ${attributeIndex + 1}`} <span className="required">*</span>
                                  </label>
                                  <select id={selectId} value={selectedValue?.tempId ?? ''}
                                    className={`form-select ${optionError ? 'error' : ''}`}
                                    aria-invalid={Boolean(optionError)}
                                    aria-describedby={optionError ? `variant-${idx}-options-error` : undefined}
                                    onChange={e => handleVariantChange(idx, 'attributeValueIds',
                                      selectOptionValue(variant.attributeValueIds || [], attribute, e.target.value))}>
                                    <option value="">-- Select value --</option>
                                    {attribute.values.map((value, valueIndex) => (
                                      <option key={value.tempId} value={value.tempId} disabled={!value.value.trim()}>
                                        {value.value.trim() || `Value ${valueIndex + 1} (empty)`}
                                      </option>
                                    ))}
                                  </select>
                                </div>
                              );
                            })}
                          </div>
                          {errors[`variant_${idx}_attributeValueIds`] && (
                            <span id={`variant-${idx}-options-error`} className="error-message" role="alert">
                              {errors[`variant_${idx}_attributeValueIds`]}
                            </span>
                          )}
                        </fieldset>
                      )}

                      {/* Variant Images */}
                      <div className="variant-images">
                        <label className="form-label">
                          <i className="fa-solid fa-images"></i>Images
                          <span className="image-count">({variant.images.length} selected)</span>
                        </label>

                        {variant.images.length > 0 && (
                          <div className="product-image-grid">
                            {variant.images.map((img, imgIdx) => (
                              <div key={imgIdx} className="product-image-item">
                                <img
                                  src={img.type === 'existing' ? img.url : img.preview}
                                  alt={`variant-${idx}-${imgIdx}`}
                                  className="product-image-thumb"
                                />
                                {img.type === 'new' && (
                                  <span className="image-new-badge">New</span>
                                )}
                                <button
                                  type="button"
                                  className="image-remove-btn"
                                  onClick={() => handleRemoveVariantImage(idx, imgIdx)}
                                  aria-label="Remove image"
                                >
                                  <i className="fa-solid fa-trash-can"></i>
                                </button>
                              </div>
                            ))}

                            <div
                              className="product-image-add"
                              onClick={() => variantFileInputRefs.current[idx]?.click()}
                              role="button"
                              tabIndex={0}
                              onKeyDown={(e) =>
                                e.key === 'Enter' && variantFileInputRefs.current[idx]?.click()
                              }
                            >
                              <span className="add-icon">+</span>
                              <span>Add more</span>
                            </div>
                          </div>
                        )}

                        {variant.images.length === 0 && (
                          <div
                            className="upload-placeholder"
                            onDragOver={(e) => handleDragOver(e)}
                            onDragLeave={(e) => handleDragLeave(e)}
                            onDrop={(e) => handleDrop(e, idx)}
                            onClick={() => variantFileInputRefs.current[idx]?.click()}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(e) =>
                              e.key === 'Enter' && variantFileInputRefs.current[idx]?.click()
                            }
                          >
                            <div className="upload-icon">
                              <i className="fa-solid fa-cloud-arrow-up"></i>
                            </div>
                            <p>Click to add variant images</p>
                            <small>JPG, PNG, GIF (max 5MB each) • Drag and drop supported</small>
                          </div>
                        )}

                        <input
                          ref={(el) => {
                            if (el) variantFileInputRefs.current[idx] = el;
                          }}
                          type="file"
                          accept="image/*"
                          multiple
                          onChange={(e) => handleAddVariantImages(idx, e)}
                          className="hidden-file-input"
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Form Actions */}
          <div className="form-actions product-form-step-actions">
            <button
              type="button"
              onClick={onCancel}
              className="btn btn-cancel"
              disabled={loading}
            >
              Cancel
            </button>
            <div className="product-form-step-navigation">
              {activeStepIndex > 0 && (
                <button type="button" className="btn btn-secondary" disabled={loading}
                  onClick={() => changeStep(FORM_STEPS[activeStepIndex - 1].id, true)}>
                  <i className="fa-solid fa-arrow-left" aria-hidden="true"></i> Back
                </button>
              )}
              {activeStepIndex < FORM_STEPS.length - 1 ? (
                <button type="button" className="btn btn-primary" disabled={loading}
                  onClick={() => changeStep(FORM_STEPS[activeStepIndex + 1].id, true)}>
                  Next <i className="fa-solid fa-arrow-right" aria-hidden="true"></i>
                </button>
              ) : (
                <button
                  key="submit-product"
                  type="submit"
                  className="btn btn-primary"
                  disabled={loading}
                >
                  <i className="fa-solid fa-paper-plane"></i>
                  <span>{loading ? 'Saving...' : product ? 'Update' : 'Create'} Product</span>
                </button>
              )}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
