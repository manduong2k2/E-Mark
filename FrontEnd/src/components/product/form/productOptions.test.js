import test from 'node:test';
import assert from 'node:assert/strict';
import { createProductOption, createOptionValue, retainOptionSelections, selectOptionValue, validateProductOptions } from './productOptions.js';
import { buildProductFormData } from './productFormData.js';

function option(code = 'color', values = ['Red', 'Blue']) {
  return {
    ...createProductOption(), code, name: code,
    values: values.map(value => ({ ...createOptionValue(), value })),
  };
}

test('tempIds are safe integers and unique across options and values', () => {
  const attributes = Array.from({ length: 20 }, () => option());
  const ids = attributes.flatMap(attribute => [attribute.tempId, ...attribute.values.map(value => value.tempId)]);
  assert.ok(ids.every(Number.isSafeInteger));
  assert.equal(new Set(ids).size, ids.length);
});

test('deleting an option value prunes selections and preserves the remaining option', () => {
  const color = option();
  const size = option('size', ['S']);
  const redId = color.values[0].tempId;
  const blueId = color.values[1].tempId;
  const sizeId = size.values[0].tempId;
  const variants = [{ sku: 'RED-S', attributeValueIds: [redId, sizeId] }, { sku: 'BLUE-S', attributeValueIds: [blueId, sizeId] }];
  const remaining = [{ ...color, values: [color.values[1]] }, size];
  assert.deepEqual(retainOptionSelections(variants, remaining).map(variant => variant.attributeValueIds), [[sizeId], [blueId, sizeId]]);
  assert.deepEqual(retainOptionSelections(variants, [size]).map(variant => variant.attributeValueIds), [[sizeId], [sizeId]]);
  assert.deepEqual(variants[0].attributeValueIds, [redId, sizeId]);
});

test('changing a selection replaces only the value belonging to that option', () => {
  const color = option();
  const selected = [color.values[0].tempId, 999];
  assert.deepEqual(selectOptionValue(selected, color, String(color.values[1].tempId)), [999, color.values[1].tempId]);
  assert.deepEqual(selectOptionValue(selected, color, ''), [999]);
});

test('validation requires names, unique codes and values, and complete variant selections', () => {
  const color = option();
  const validVariant = { attributeValueIds: [color.values[0].tempId] };
  assert.deepEqual(validateProductOptions([color], [validVariant]), {});
  assert.deepEqual(validateProductOptions([], [{}]), {});
  assert.ok(validateProductOptions([color], [{}]).variant_0_attributeValueIds);
  assert.ok(validateProductOptions([color], [{ attributeValueIds: color.values.map(value => value.tempId) }]).variant_0_attributeValueIds);
  const duplicate = { ...color, code: ' COLOR ', values: color.values.map(value => ({ ...value, value: 'Red' })) };
  const errors = validateProductOptions([color, duplicate], []);
  assert.ok(errors['attributes[1].code']);
  assert.ok(errors['attributes[1].values[1].value']);
  const blank = validateProductOptions([createProductOption()], []);
  assert.ok(blank['attributes[0].name']);
  assert.ok(blank['attributes[0].code']);
  assert.ok(blank['attributes[0].values[0].value']);
});

test('optional icon accepts images and rejects oversized or non-image files', () => {
  const attribute = option();
  assert.deepEqual(validateProductOptions([attribute], []), {});
  attribute.icon = { type: 'text/plain', size: 10 };
  assert.ok(validateProductOptions([attribute], [])['attributes[0].icon']);
  attribute.icon = { type: 'image/png', size: 5 * 1024 * 1024 + 1 };
  assert.ok(validateProductOptions([attribute], [])['attributes[0].icon']);
});

test('create multipart matches nested backend fields and references the same tempIds', () => {
  const color = option();
  const size = option('size', ['S', 'M']);
  color.icon = new File(['icon'], 'color.png', { type: 'image/png' });
  color.description = ' Color swatch ';
  color.values[0].value = ' Red ';
  const image = new File(['photo'], 'red.png', { type: 'image/png' });
  const fd = buildProductFormData({
    name: 'Shirt', brandId: 'brand-id', attributes: [color, size],
    variants: [{ name: 'Red / S', sku: 'RED-S', price: 10, stock: 2,
      attributeValueIds: [color.values[0].tempId, size.values[0].tempId], images: [{ type: 'new', file: image }] }],
  });
  assert.equal(fd.get('attributes[0].code'), 'color');
  assert.equal(fd.get('attributes[0].description'), 'Color swatch');
  assert.equal(fd.get('attributes[0].values[0].value'), 'Red');
  assert.equal(fd.get('attributes[0].values[0].tempId'), fd.get('variants[0].attributeValueIds[0]'));
  assert.equal(fd.get('attributes[1].values[0].tempId'), fd.get('variants[0].attributeValueIds[1]'));
  assert.equal(fd.get('attributes[0].icon'), color.icon);
  assert.equal(fd.get('attributes[1].icon'), null);
  assert.equal(fd.get('variants[0].images'), image);
});

test('edit multipart preserves images and leaves unsupported option fields out', () => {
  const fd = buildProductFormData({ attributes: [option()], variants: [{
    id: 'variant-id', name: 'Variant', sku: 'SKU', price: 10, stock: 2, attributeValueIds: [123],
    images: [{ type: 'existing', id: 'image-id', url: '/image.png' }],
  }] }, { keepExistingImages: true });
  assert.equal(fd.get('variants[0].images[0].id'), 'image-id');
  assert.ok([...fd.keys()].every(key => !key.startsWith('attributes') && !key.includes('attributeValueIds')));
});
