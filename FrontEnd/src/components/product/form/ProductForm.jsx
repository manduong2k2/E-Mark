import { useState, useRef, useEffect } from 'react';
import CategoryTreeSelect from '../../category/tree/CategoryTreeSelect';
import SearchableVendorSelect from '../../vendor/select/SearchableVendorSelect';
import './ProductForm.css';

export default function ProductForm({
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

  const [formData, setFormData] = useState({
    name: product?.name || '',
    description: product?.description || '',
    brandId: product?.brandId || product?.brand?.id || '',
    vendorId: product?.vendorId || product?.vendor?.id || '',
    categoryIds: product?.categoryIds || product?.categories?.map((c) => c.id) || [],
    status: product?.status || defaultStatus,
    options: product?.options?.map((opt, idx) => ({ ...opt, tempId: idx })) || [],
    variants: product?.variants?.map((v) => ({
      ...v,
      tempId: v.tempId || Date.now() + Math.random(),
      images: v.images || [],
      optionIds: v.optionIds || [],
    })) || [
        {
          tempId: Date.now(),
          name: '',
          price: '',
          stock: '',
          sku: '',
          optionIds: [],
          images: [],
        },
      ],
  });

  const [nextOptionTempId, setNextOptionTempId] = useState(
    product?.options?.length || 0
  );

  const [errors, setErrors] = useState({});
  const variantFileInputRefs = useRef({});

  // Update form data when product prop changes
  useEffect(() => {
    if (product) {
      const mappedOptions = product.options?.map((opt, idx) => ({
        tempId: idx,
        name: opt.name || '',
        value: opt.value || '',
      })) || [];

      const mappedVariants = product.variants?.map((v) => {
        const variantOptionIds = v.options?.map((varOpt) =>
          mappedOptions.findIndex(o => o.name === varOpt.name)
        ).filter(idx => idx !== -1) || [];

        return {
          tempId: v.id || Date.now() + Math.random(),
          name: v.name || '',
          price: v.price ?? '',
          stock: v.stock ?? '',
          sku: v.sku || '',
          optionIds: variantOptionIds,
          images: Array.isArray(v.images)
            ? v.images.map((url) => ({
              type: 'existing',
              url: typeof url === 'string' ? url : url.url
            }))
            : [],
        };
      }) || [];

      const newFormData = {
        name: product.name || '',
        description: product.description || '',
        brandId: product.brandId || product.brand?.id || '',
        categoryIds: product.categoryIds || product.categories?.map((c) => c.id) || [],
        status: product.status || defaultStatus,
        options: mappedOptions,
        variants: mappedVariants.length > 0 ? mappedVariants : [
          {
            tempId: Date.now(),
            name: '',
            price: '',
            stock: '',
            sku: '',
            optionIds: [],
            images: [],
          },
        ],
      };

      setFormData(newFormData);
      setNextOptionTempId(product.options?.length || 0);
    }
  }, [product, defaultStatus]);

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
    const newErrors = {};
    if (!formData.name.trim()) newErrors.name = 'Product name is required';
    if (!formData.brandId) newErrors.brandId = 'Please select a brand';

    if (!formData.variants || formData.variants.length === 0) {
      newErrors.variants = 'At least one variant is required';
    } else {
      formData.variants.forEach((v, idx) => {
        if (!v.name?.trim()) newErrors[`variant_${idx}_name`] = 'Variant name is required';
        if (v.price === '' || isNaN(Number(v.price)) || Number(v.price) < 0)
          newErrors[`variant_${idx}_price`] = 'Invalid price (must be ≥ 0)';
        if (v.stock === '' || isNaN(Number(v.stock)) || Number(v.stock) < 0)
          newErrors[`variant_${idx}_stock`] = 'Invalid stock (must be ≥ 0)';
        if (!v.sku?.trim()) newErrors[`variant_${idx}_sku`] = 'SKU is required';
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    const serverErrors = await onSubmit({
      ...formData,
      options: formData.options,
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
          mappedKey = mappedKey.replace(/options\[(\d+)\]\./, 'option_$1_');
          mappedErrors[mappedKey] = value;
        });
        // Bind field errors
        setErrors(mappedErrors);
      } else if (serverErrors._ || serverErrors.message) {
        // Legacy format or simple message
        const errorMessage = serverErrors._ || serverErrors.message;
        window.showError(errorMessage, 'Validation Error');
        const { _: _ignored, message: _msgIgnored, ...fieldErrors } = serverErrors;
        if (Object.keys(fieldErrors).length > 0) {
          setErrors(fieldErrors);
        }
      } else {
        // Field errors only
        setErrors(serverErrors);
      }
    }
  };

  const handleAddOption = () => {
    const newOption = {
      tempId: nextOptionTempId,
      name: '',
      value: '',
    };
    setFormData((prev) => ({
      ...prev,
      options: [...prev.options, newOption],
    }));
    setNextOptionTempId((prev) => prev + 1);
  };

  const handleRemoveOption = (index) => {
    setFormData((prev) => {
      const removedOptionTempId = prev.options[index].tempId;
      const updatedOptions = prev.options.filter((_, i) => i !== index);
      const updatedVariants = prev.variants.map((variant) => ({
        ...variant,
        optionIds: variant.optionIds.filter((id) => id !== removedOptionTempId),
      }));
      return { ...prev, options: updatedOptions, variants: updatedVariants };
    });
  };

  const handleOptionChange = (index, field, value) => {
    setFormData((prev) => {
      const updatedOptions = [...prev.options];
      updatedOptions[index] = { ...updatedOptions[index], [field]: value };
      return { ...prev, options: updatedOptions };
    });
    if (errors[`option_${index}_${field}`]) {
      setErrors((prev) => ({ ...prev, [`option_${index}_${field}`]: '' }));
    }
  };

  const handleAddVariant = () => {
    const newVariant = {
      tempId: Date.now() + Math.random(),
      name: '',
      price: '',
      stock: '',
      sku: '',
      optionIds: [],
      images: [],
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

  const handleVariantOptionToggle = (variantIndex, optionTempId) => {
    setFormData((prev) => {
      const updatedVariants = [...prev.variants];
      const variant = updatedVariants[variantIndex];

      if (variant.optionIds.includes(optionTempId)) {
        variant.optionIds = variant.optionIds.filter((id) => id !== optionTempId);
      } else {
        variant.optionIds = [...variant.optionIds, optionTempId];
      }

      return { ...prev, variants: updatedVariants };
    });
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
        <form onSubmit={handleSubmit} className="product-form">
          <span>
            {/* Section 1: Basic Information */}
            <div className="form-section">
              <div className="section-header">
                <div>
                  <h3 className="section-title">
                    <i className="fa-solid fa-circle-info"></i> Basic Information
                  </h3>
                  <p className="section-subtitle">Product name, brand, and category</p>
                </div>
                <span className="step-badge">Step 1</span>
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

            <br />

            {/* Section 2: Product Options */}
            <div className="form-section">
              <div className="section-header">
                <div>
                  <h3 className="section-title">
                    <i className="fa-solid fa-sliders"></i> Product Options
                  </h3>
                  <p className="section-subtitle">e.g. Color, Size, RAM with automatic tempId mapping</p>
                </div>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleAddOption}
                >
                  <i className="fa-solid fa-plus"></i> Add Option
                </button>
              </div>

              {formData.options.length === 0 ? (
                <p className="empty-message">No options added yet. Click "+ Add Option" to create one.</p>
              ) : (
                <div className="options-list">
                  {formData.options.map((option, idx) => (
                    <div key={option.tempId || idx} className="option-item">
                      <div className='w-100 d-flex justify-content-between'>
                        <div className="option-tempid">#{option.tempId}</div>
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => handleRemoveOption(idx)}>
                          <i className="fa-solid fa-trash-can"></i>
                        </button>
                      </div>
                      <div className="w-100 option-fields">
                        <div className="form-group">
                          <label className="form-label">Option Name</label>
                          <input
                            type="text"
                            placeholder="e.g. color, size"
                            value={option.name}
                            onChange={(e) => handleOptionChange(idx, 'name', e.target.value)}
                            className={`form-input form-input-sm ${errors[`option_${idx}_name`] ? 'error' : ''}`}
                          />
                          {errors[`option_${idx}_name`] && (
                            <span className="error-message">{errors[`option_${idx}_name`]}</span>
                          )}
                        </div>
                        <div className="form-group">
                          <label className="form-label">Option Value</label>
                          <input
                            type="text"
                            placeholder="e.g. S, Blue"
                            value={option.value}
                            onChange={(e) => handleOptionChange(idx, 'value', e.target.value)}
                            className={`form-input form-input-sm ${errors[`option_${idx}_value`] ? 'error' : ''}`}
                          />
                          {errors[`option_${idx}_value`] && (
                            <span className="error-message">{errors[`option_${idx}_value`]}</span>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </span>

          <span>
            {/* Section 3: Product Variants */}
            <div className="form-section">
              <div className="section-header">
                <div>
                  <h3 className="section-title">
                    <i className="fa-solid fa-cubes"></i> Product Variants
                  </h3>
                  <p className="section-subtitle">Manage stock items, associate options, and upload multiple variant photos</p>
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

                      {/* Selected Options Mapping */}
                      {formData.options.length > 0 && (
                        <div className="variant-options">
                          <label className="form-label">Selected Options</label>
                          <div className="options-checkboxes">
                            {formData.options.map((option) => (
                              <label key={option.tempId} className="option-checkbox">
                                <input
                                  type="checkbox"
                                  checked={(variant.optionIds || []).includes(option.tempId)}
                                  onChange={() => handleVariantOptionToggle(idx, option.tempId)}
                                />
                                <span>
                                  <i className="fa-solid fa-check"></i>
                                  ID {option.tempId}: <strong>{option.name}</strong> ({option.value})
                                </span>
                              </label>
                            ))}
                          </div>
                        </div>
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

            {/* Form Actions */}
            <div className="form-actions">
              <button
                type="button"
                onClick={onCancel}
                className="btn btn-cancel"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
              >
                <i className="fa-solid fa-paper-plane"></i>
                <span>{loading ? 'Saving...' : product ? 'Update' : 'Create'} Product</span>
              </button>
            </div>
          </span>
        </form>
      </div>
    </div>
  );
}
