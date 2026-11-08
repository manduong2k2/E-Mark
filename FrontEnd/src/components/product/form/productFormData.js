// Multipart body for POST /api/products and PUT /api/products/:id from ProductForm's values.
// Field names follow the Spring binding of CreateProductRequest / UpdateProductRequest
// (variants[0].name, variants[0].images[0].id, ...).

export function buildProductFormData(formData, { keepExistingImages = false } = {}) {
  const fd = new FormData();
  ['name', 'description', 'brandId', 'vendorId', 'status'].forEach(key => fd.append(key, formData[key] ?? ''));
  formData.categoryIds?.forEach(id => fd.append('categoryIds', id));

  // UpdateProductRequest does not accept attributes yet.
  if (!keepExistingImages) {
    formData.attributes?.forEach((attribute, index) => {
      const path = `attributes[${index}]`;
      fd.append(`${path}.code`, attribute.code.trim());
      fd.append(`${path}.name`, attribute.name.trim());
      fd.append(`${path}.description`, attribute.description?.trim() ?? '');
      if (attribute.icon) fd.append(`${path}.icon`, attribute.icon);
      attribute.values.forEach((value, valueIndex) => {
        fd.append(`${path}.values[${valueIndex}].tempId`, value.tempId);
        fd.append(`${path}.values[${valueIndex}].value`, value.value.trim());
      });
    });
  }

  formData.variants?.forEach((variant, idx) => {
    fd.append(`variants[${idx}].id`, variant.id ?? '');
    fd.append(`variants[${idx}].name`, variant.name);
    fd.append(`variants[${idx}].price`, variant.price);
    fd.append(`variants[${idx}].stock`, variant.stock);
    fd.append(`variants[${idx}].sku`, variant.sku);
    if (!keepExistingImages) {
      variant.attributeValueIds?.forEach((id, valueIndex) =>
        fd.append(`variants[${idx}].attributeValueIds[${valueIndex}]`, id));
    }

    if (keepExistingImages) {
      variant.images?.forEach((img, imgIdx) => {
        fd.append(`variants[${idx}].images[${imgIdx}].id`, img.type === 'existing' ? (img.id ?? '') : '');
        if (img.type === 'new' && img.file) {
          fd.append(`variants[${idx}].images[${imgIdx}].file`, img.file);
        }
      });
    } else {
      variant.images?.forEach(img => {
        if (img.type === 'new' && img.file) fd.append(`variants[${idx}].images`, img.file);
      });
    }
  });

  return fd;
}

/** ProductForm's onSubmit result for a failed save: { message, errors } (field errors are bound in the form). */
export const productSaveError = (data, fallback) => ({
  message: data?.message || fallback,
  errors: data?.errors || {},
});
