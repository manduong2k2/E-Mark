import test from 'node:test';
import assert from 'node:assert/strict';
import { cloneProductVariant } from './productVariants.js';
import { buildProductFormData } from './productFormData.js';

function sourceVariant(images = []) {
  return {
    id: 'saved-variant-id', tempId: 'original', name: 'Red / Large',
    price: 125, stock: 8, sku: 'RED-L', attributeValueIds: [11, 22], images,
  };
}

test('clone prefills fields and keeps edits independent from the source', async () => {
  const file = new File(['image'], 'photo.png', { type: 'image/png' });
  const source = sourceVariant([{ type: 'new', file, preview: 'data:image/png;base64,aGVsbG8=' }]);
  const clone = await cloneProductVariant(source);

  assert.equal(clone.id, undefined);
  assert.notEqual(clone.tempId, source.tempId);
  for (const field of ['name', 'price', 'stock', 'sku']) assert.equal(clone[field], source[field]);
  assert.deepEqual(clone.attributeValueIds, source.attributeValueIds);
  assert.equal(clone.images[0].file, file);
  assert.equal(clone.images[0].preview, source.images[0].preview);
  clone.name = 'Blue / Large';
  clone.attributeValueIds.pop();
  clone.images[0].preview = 'changed';
  clone.images.push({ type: 'new', file });
  assert.equal(source.name, 'Red / Large');
  assert.deepEqual(source.attributeValueIds, [11, 22]);
  assert.equal(source.images.length, 1);
  assert.equal(source.images[0].preview, 'data:image/png;base64,aGVsbG8=');
});

test('cloning saved images uploads copies without reusing variant or image IDs', async () => {
  const image = { type: 'existing', id: 'saved-image-id', url: 'data:image/png;base64,aGVsbG8=' };
  const source = sourceVariant([image]);
  const clone = await cloneProductVariant(source);
  assert.equal(clone.images[0].type, 'new');
  assert.equal(clone.images[0].id, undefined);
  assert.equal(clone.images[0].file.type, 'image/png');
  assert.match(clone.images[0].file.name, /\.png$/);
  assert.equal(await clone.images[0].file.text(), 'hello');

  for (const keepExistingImages of [false, true]) {
    const fd = buildProductFormData({ variants: [source, clone] }, { keepExistingImages });
    assert.equal(fd.get('variants[0].id'), 'saved-variant-id');
    assert.equal(fd.get('variants[1].id'), '');
    const uploaded = fd.get(keepExistingImages ? 'variants[1].images[0].file' : 'variants[1].images');
    assert.equal(await uploaded.text(), 'hello');
    if (keepExistingImages) {
      assert.equal(fd.get('variants[0].images[0].id'), 'saved-image-id');
      assert.equal(fd.get('variants[1].images[0].id'), '');
    }
  }
  assert.equal(source.images[0], image);
  assert.equal(source.images[0].type, 'existing');
});

test('repeated clones have distinct identities', async () => {
  const source = sourceVariant();
  const clones = await Promise.all([cloneProductVariant(source), cloneProductVariant(source)]);
  assert.notEqual(clones[0].tempId, clones[1].tempId);
});

test('failed image copies reject without modifying the source', async () => {
  for (const url of ['data:text/plain,not-an-image', 'invalid-url']) {
    const source = sourceVariant([{ type: 'existing', id: 'saved-image-id', url }]);
    await assert.rejects(cloneProductVariant(source));
    assert.equal(source.id, 'saved-variant-id');
    assert.equal(source.images[0].id, 'saved-image-id');
    assert.equal(source.images[0].type, 'existing');
  }
});
