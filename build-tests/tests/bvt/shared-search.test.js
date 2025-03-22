import ava from 'ava';
import test from '../../helpers/test';
import { AdminApi } from '../../helpers/Api';
import $ from '../../payloads';
import isDate from '../../helpers/isDate';
import isArray from '../../helpers/isArray';
import isString from '../../helpers/isString';
import isNumber from '../../helpers/isNumber';

const newSharedSearchCodes = [];

ava.always.after('[bvt] Remove shared searches created in tests', async () => {
  const adminApi = await AdminApi.loggedIn();
  for (const code of newSharedSearchCodes) {
    await adminApi.sharedSearches.delete(code);
  }
});

test('[bvt] Can list shared searches', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const sharedSearches = await adminApi.sharedSearches.list('ordersScope');
  t.truthy(isArray(sharedSearches));
  for (const sharedSearch of sharedSearches) {
    t.truthy(isNumber(sharedSearch.id));
    t.truthy(isNumber(sharedSearch.storeAdminId));
    t.truthy(isString(sharedSearch.code));
    t.truthy(isString(sharedSearch.title));
    t.truthy(isString(sharedSearch.scope));
    t.truthy(isDate(sharedSearch.createdAt));
    t.truthy(sharedSearch.query);
    t.truthy(sharedSearch.rawQuery);
  }
});

test('[bvt] Can create shared search', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomSharedSearchPayload();
  const newSharedSearch = await adminApi.sharedSearches.create(payload);
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  t.truthy(isNumber(newSharedSearch.id));
  t.truthy(isNumber(newSharedSearch.storeAdminId));
  t.truthy(isString(newSharedSearch.title));
  t.truthy(isString(newSharedSearch.scope));
  t.truthy(isDate(newSharedSearch.createdAt));
  t.truthy(newSharedSearch.query);
  t.truthy(newSharedSearch.rawQuery);
});

test('[bvt] Can view shared search details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomSharedSearchPayload();
  const newSharedSearch = await adminApi.sharedSearches.create(payload);
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const foundSharedSearch = await adminApi.sharedSearches.one(newSharedSearch.code);
  t.deepEqual(foundSharedSearch, newSharedSearch);
});

test('[bvt] Can delete shared search', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomSharedSearchPayload();
  const newSharedSearch = await adminApi.sharedSearches.create(payload);
  await adminApi.sharedSearches.delete(newSharedSearch.code);
  try {
    await adminApi.sharedSearches.one(newSharedSearch.code);
    t.fail('Shared search was found after deletion.');
  } catch (error) {
    if (error && error.response) {
      t.is(error.response.status, 404);
      t.truthy(error.response.clientError);
      t.falsy(error.response.serverError);
    } else {
      throw error;
    }
  }
});

test('[bvt] Can list associates', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSharedSearch = await adminApi.sharedSearches.create($.randomSharedSearchPayload());
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const associates = await adminApi.sharedSearches.getAssociates(newSharedSearch.code);
  t.truthy(isArray(associates));
  for (const associate of associates) {
    t.truthy(isNumber(associate.id));
    t.truthy(isString(associate.email));
    t.truthy(isString(associate.name));
    t.truthy(isDate(associate.createdAt));
  }
});

test('[bvt] Can add associate', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSharedSearch = await adminApi.sharedSearches.create($.randomSharedSearchPayload());
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const newStoreAdmin = await adminApi.storeAdmins.create($.randomStoreAdminPayload());
  const associationPayload = { associates: [newStoreAdmin.id] };
  const updatedSharedSearch = await adminApi.sharedSearches
    .addAssociate(newSharedSearch.code, associationPayload).then(r => r.result);
  t.deepEqual(updatedSharedSearch, newSharedSearch);
  const associates = await adminApi.sharedSearches.getAssociates(newSharedSearch.code);
  t.truthy(associates.length > 0);
  const newAssociate = associates.find(a => a.id === newStoreAdmin.id);
  t.is(newAssociate.id, newStoreAdmin.id);
  t.is(newAssociate.name, newStoreAdmin.name);
  t.is(newAssociate.email, newStoreAdmin.email);
});

test('[bvt] Can remove associate', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newSharedSearch = await adminApi.sharedSearches.create($.randomSharedSearchPayload());
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const newStoreAdmin = await adminApi.storeAdmins.create($.randomStoreAdminPayload());
  const associationPayload = { associates: [newStoreAdmin.id] };
  await adminApi.sharedSearches.addAssociate(newSharedSearch.code, associationPayload);
  await adminApi.sharedSearches.removeAssociate(newSharedSearch.code, newStoreAdmin.id);
  const associates = await adminApi.sharedSearches.getAssociates(newSharedSearch.code);
  t.falsy(associates.find(associate => associate.id === newStoreAdmin.id));
});
