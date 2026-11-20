// A clone must upload its own images instead of reusing another variant's image IDs.
export async function cloneProductVariant(variant) {
  const images = await Promise.all(variant.images.map(async (image, index) => {
    if (image.type === 'new') return { ...image };

    const response = await fetch(image.url);
    if (!response.ok) throw new Error('Unable to copy variant images. Please try again.');
    const blob = await response.blob();
    if (!blob.type.startsWith('image/')) {
      throw new Error('Unable to copy variant images. Please try again.');
    }
    const extension = blob.type === 'image/svg+xml' ? 'svg' : blob.type.slice(6).replace(/[^a-z0-9]/gi, '');
    return {
      type: 'new',
      file: new File([blob], `variant-image-${index + 1}.${extension}`, { type: blob.type }),
      preview: image.url,
    };
  }));

  return {
    tempId: crypto.randomUUID(),
    name: variant.name,
    price: variant.price,
    stock: variant.stock,
    sku: variant.sku,
    images,
    attributeValueIds: [...(variant.attributeValueIds || [])],
  };
}
