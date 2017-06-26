import test from '../helpers/test';
import testNotes from './testNotes';
import { AdminApi } from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';
import waitFor from '../helpers/waitFor';
import $ from '../payloads';

test('Can create a new SKU', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomSkuPayload();
  const newSku = await adminApi.skus.create('default', payload);
  t.truthy(isNumber(newSku.id));
  t.truthy(newSku.context);
  t.is(newSku.context.name, 'default');
  t.deepEqual(newSku.attributes, payload.attributes);
  t.deepEqual(newSku.albums, payload.albums || []);
});

test('Can view SKU details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSku = await adminApi.skus.create('default', $.randomSkuPayload());
  const foundSku = await adminApi.skus.one('default', newSku.attributes.code.v);
  t.deepEqual(foundSku, newSku);
});

test('Can update SKU details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSku = await adminApi.skus.create('default', $.randomSkuPayload());
  const payload = $.randomSkuPayload();
  const updatedSku = await adminApi.skus.update('default', newSku.attributes.code.v, payload);
  t.truthy(isNumber(updatedSku.id));
  t.truthy(updatedSku.context);
  t.is(updatedSku.context.name, 'default');
  t.deepEqual(updatedSku.attributes, payload.attributes);
  t.deepEqual(updatedSku.albums, payload.albums || []);
});

test('Can archive a SKU', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSku = await adminApi.skus.create('default', $.randomSkuPayload());
  const archivedSku = await adminApi.skus.archive('default', newSku.attributes.code.v);
  t.truthy(isDate(archivedSku.archivedAt));
  t.is(archivedSku.id, newSku.id);
  t.deepEqual(archivedSku.attributes, newSku.attributes);
  t.deepEqual(archivedSku.albums, newSku.albums);
  t.deepEqual(archivedSku.context, newSku.context);
});

test('Can access the inventory', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSku = await adminApi.skus.create('default', $.randomSkuPayload());
  const inventory = await waitFor(500, 10000, () => adminApi.skus.inventory(newSku.attributes.code.v));
  t.truthy(isArray(inventory.summary));
  t.is(inventory.summary.length, 4);
  const inventoryItemTypes = inventory.summary.map(item => item.type);
  t.deepEqual(inventoryItemTypes.sort(), ['Sellable', 'Non-sellable', 'Backorder', 'Preorder'].sort());
  for (const item of inventory.summary) {
    t.is(item.sku, newSku.attributes.code.v);
    t.truthy(item.stockItem);
    t.truthy(item.stockLocation);
    t.is(item.onHand, 0);
    t.is(item.onHold, 0);
    t.is(item.reserved, 0);
    t.is(item.shipped, 0);
    t.is(item.afs, 0);
    t.is(item.afsCost, 0);
    t.truthy(isDate(item.createdAt));
  }
});

testNotes({
  objectType: 'sku',
  createObject: adminApi => adminApi.skus.create('default', $.randomSkuPayload()),
  selectId: sku => sku.attributes.code.v,
});
