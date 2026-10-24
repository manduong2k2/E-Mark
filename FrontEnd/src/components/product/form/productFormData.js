// Multipart body for POST /api/products and PUT /api/products/:id from ProductForm's values.
// Field names follow the Spring binding of CreateProductRequest / UpdateProductRequest
// (variants[0].name, variants[0].optionIds[1], ...).

export function buildProductFormData(formData, { keepExistingImages = false } = {}) {
  const fd = new FormData();
  ['name', 'description', 'brandId', 'vendorId', 'status'].forEach(key => fd.append(key, formData[key] ?? ''));
  formData.categoryIds?.forEach(id => fd.append('categoryIds', id));

  formData.options?.forEach((option, idx) => {
    fd.append(`options[${idx}].tempId`, option.tempId);
    fd.append(`options[${idx}].name`, option.name);
    fd.append(`options[${idx}].value`, option.value);
  });

  formData.variants?.forEach((variant, idx) => {
    fd.append(`variants[${idx}].name`, variant.name);
    fd.append(`variants[${idx}].price`, variant.price);
    fd.append(`variants[${idx}].stock`, variant.stock);
    fd.append(`variants[${idx}].sku`, variant.sku);
    variant.optionIds?.forEach((optionId, optionIdx) => {
      fd.append(`variants[${idx}].optionIds[${optionIdx}]`, optionId);
    });

    // Edit: images already stored are sent back by URL so they are kept
    if (keepExistingImages) {
      variant.images?.forEach((img, imgIdx) => {
        if (img.type === 'existing' && img.url) fd.append(`variants[${idx}].imageUrls[${imgIdx}]`, img.url);
      });
    }
    variant.images?.forEach(img => {
      if (img.type === 'new' && img.file) fd.append(`variants[${idx}].images`, img.file);
    });
  });

  return fd;
}

/** ProductForm's onSubmit result for a failed save: { message, errors } (field errors are bound in the form). */
export const productSaveError = (data, fallback) => ({
  message: data?.message || fallback,
  errors: data?.errors || {},
});
