import test from '../helpers/test';
import testNotes from './testNotes';
import { AdminApi } from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isString from '../helpers/isString';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';
import $ from '../payloads';

test('Can create a product', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomProductPayload();
  const newProduct = await adminApi.products.create('default', payload);
  t.truthy(isNumber(newProduct.id));
  t.is(newProduct.slug, payload.attributes.title.v.toLowerCase());
  t.truthy(newProduct.context);
  t.is(newProduct.context.name, 'default');
  t.deepEqual(newProduct.attributes, payload.attributes);
  t.deepEqual(newProduct.albums, payload.albums || []);
  t.deepEqual(newProduct.variants, payload.variants || []);
  t.deepEqual(newProduct.taxons, payload.taxons || []);
  for (let i = 0; i < payload.skus.length; i += 1) {
    const payloadSku = payload.skus[i];
    const productSku = newProduct.skus[i];
    t.truthy(isNumber(productSku.id));
    t.deepEqual(productSku.context, payloadSku.context);
    t.deepEqual(productSku.attributes, payloadSku.attributes);
    t.deepEqual(productSku.albums, payloadSku.albums || []);
  }
});

test('Can archive a product', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const archivedProduct = await adminApi.products.archive('default', newProduct.id);
  t.truthy(isDate(archivedProduct.archivedAt));
});

test('Can view product details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const foundProduct = await adminApi.products.one('default', newProduct.id);
  t.deepEqual(foundProduct, newProduct);
});

test('Can update product details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const payload = $.randomProductPayload();
  const updatedProduct = await adminApi.products.update('default', newProduct.id, payload);
  t.truthy(isNumber(updatedProduct.id));
  t.truthy(updatedProduct.context);
  t.is(updatedProduct.context.name, 'default');
  t.deepEqual(updatedProduct.attributes, payload.attributes);
  t.deepEqual(updatedProduct.albums, payload.albums || []);
  t.deepEqual(updatedProduct.variants, payload.variants || []);
  t.deepEqual(updatedProduct.taxons, payload.taxons || []);
  for (let i = 0; i < payload.skus.length; i += 1) {
    const payloadSku = payload.skus[i];
    const productSku = updatedProduct.skus[i];
    t.truthy(isNumber(productSku.id));
    t.deepEqual(productSku.context, payloadSku.context);
    t.deepEqual(productSku.attributes, payloadSku.attributes);
    t.deepEqual(productSku.albums, payloadSku.albums || []);
  }
});

test('Can create an album', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const payload = $.randomAlbumPayload();
  const newAlbum = await adminApi.productAlbums.create('default', newProduct.id, payload);
  t.is(newAlbum.name, payload.name);
  t.truthy(isNumber(newAlbum.id));
  t.truthy(isArray(newAlbum.images));
  t.truthy(isDate(newAlbum.createdAt));
  t.truthy(isDate(newAlbum.updatedAt));
  for (const image of newAlbum.images) {
    t.truthy(isNumber(image.id));
    t.truthy(isString(image.src));
    t.truthy(isString(image.alt));
    t.truthy(isString(image.title));
  }
});

test('Can update album details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const newAlbum = await adminApi.productAlbums.create('default', newProduct.id, $.randomAlbumPayload());
  const payload = $.randomAlbumPayload();
  const updatedAlbum = await adminApi.albums.update('default', newAlbum.id, payload);
  t.is(updatedAlbum.id, newAlbum.id);
  t.is(updatedAlbum.createdAt, newAlbum.createdAt);
  t.is(updatedAlbum.name, payload.name);
  // t.not(updatedAlbum.updatedAt, newAlbum.updatedAt);
  t.truthy(isArray(updatedAlbum.images));
  for (let i = 0; i < updatedAlbum.images.length; i += 1) {
    const albumImage = updatedAlbum.images[i];
    const payloadImage = payload.images[i];
    t.truthy(isNumber(albumImage.id));
    t.is(albumImage.src, payloadImage.src);
    t.is(albumImage.alt, payloadImage.alt);
    t.is(albumImage.title, payloadImage.title);
  }
});

test('Can archive an album', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const newAlbum = await adminApi.productAlbums.create('default', newProduct.id, $.randomAlbumPayload());
  const archivedAlbum = await adminApi.albums.archive('default', newAlbum.id);
  t.truthy(isDate(archivedAlbum.archivedAt));
});

test('Can upload an image', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const newAlbum = await adminApi.productAlbums.create('default', newProduct.id, $.randomAlbumPayload());
  const updatedAlbum = await adminApi.albums.uploadImages('default', newAlbum.id, [$.testImagePath]);
  t.is(updatedAlbum.id, newAlbum.id);
  t.is(updatedAlbum.name, newAlbum.name);
  t.truthy(isDate(updatedAlbum.createdAt));
  t.truthy(isDate(updatedAlbum.updatedAt));
  t.truthy(isArray(updatedAlbum.images));
  t.is(updatedAlbum.images.length, newAlbum.images.length + 1);
  const newImage = updatedAlbum.images[newAlbum.images.length];
  t.truthy(isNumber(newImage.id));
  t.truthy(isString(newImage.src));
  t.truthy(isString(newImage.alt));
  t.truthy(isString(newImage.title));
});

test('Can update image details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const newAlbum = await adminApi.productAlbums.create('default', newProduct.id, $.randomAlbumPayload({ minImages: 1 }));
  const payload = $.randomImagePayload();
  newAlbum.images[0] = payload;
  const updatedAlbum = await adminApi.albums.update('default', newAlbum.id, newAlbum);
  t.truthy(isNumber(updatedAlbum.images[0].id));
  t.is(updatedAlbum.images[0].src, payload.src);
  t.is(updatedAlbum.images[0].alt, payload.alt);
  t.is(updatedAlbum.images[0].title, payload.title);
});

test('Can delete an image', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newProduct = await adminApi.products.create('default', $.randomProductPayload());
  const newAlbum = await adminApi.productAlbums.create('default', newProduct.id, $.randomAlbumPayload({ minImages: 1 }));
  const imageCount = newAlbum.images.length;
  newAlbum.images.pop();
  const updatedAlbum = await adminApi.albums.update('default', newAlbum.id, newAlbum);
  t.is(updatedAlbum.images.length, imageCount - 1);
});

testNotes({
  objectType: 'product',
  createObject: adminApi => adminApi.products.create('default', $.randomProductPayload()),
});
