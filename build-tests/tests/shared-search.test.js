import ava from 'ava';
import test from '../helpers/test';
import Api from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';

const newSharedSearchCodes = [];

ava.always.after('Remove shared searches created in tests', async () => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  for (const code of newSharedSearchCodes) {
    await api.sharedSearches.delete(code);
  }
});

test('Can list shared searches', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const sharedSearches = await api.sharedSearches.list('ordersScope');
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

test('Can create shared search', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const payload = $.randomSharedSearchPayload();
  const newSharedSearch = await api.sharedSearches.create(payload);
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

test('Can view shared search details', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const payload = $.randomSharedSearchPayload();
  const newSharedSearch = await api.sharedSearches.create(payload);
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const foundSharedSearch = await api.sharedSearches.one(newSharedSearch.code);
  t.deepEqual(foundSharedSearch, newSharedSearch);
});

test('Can delete shared search', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const payload = $.randomSharedSearchPayload();
  const newSharedSearch = await api.sharedSearches.create(payload);
  await api.sharedSearches.delete(newSharedSearch.code);
  try {
    await api.sharedSearches.one(newSharedSearch.code);
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

test('Can list associates', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newSharedSearch = await api.sharedSearches.create($.randomSharedSearchPayload());
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const associates = await api.sharedSearches.getAssociates(newSharedSearch.code);
  t.truthy(isArray(associates));
  for (const associate of associates) {
    t.truthy(isNumber(associate.id));
    t.truthy(isString(associate.email));
    t.truthy(isString(associate.name));
    t.truthy(isDate(associate.createdAt));
  }
});

test('Can add associate', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newSharedSearch = await api.sharedSearches.create($.randomSharedSearchPayload());
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const newStoreAdmin = await api.storeAdmins.create($.randomStoreAdminPayload());
  const associationPayload = { associates: [newStoreAdmin.id] };
  const updatedSharedSearch = await api.sharedSearches
    .addAssociate(newSharedSearch.code, associationPayload).then(r => r.result);
  t.deepEqual(updatedSharedSearch, newSharedSearch);
  const associates = await api.sharedSearches.getAssociates(newSharedSearch.code);
  t.truthy(associates.length > 0);
  const newAssociate = associates.find(a => a.id === newStoreAdmin.id);
  t.is(newAssociate.id, newStoreAdmin.id);
  t.is(newAssociate.name, newStoreAdmin.name);
  t.is(newAssociate.email, newStoreAdmin.email);
});

test('Can remove associate', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newSharedSearch = await api.sharedSearches.create($.randomSharedSearchPayload());
  t.truthy(isString(newSharedSearch.code));
  newSharedSearchCodes.push(newSharedSearch.code);
  const newStoreAdmin = await api.storeAdmins.create($.randomStoreAdminPayload());
  const associationPayload = { associates: [newStoreAdmin.id] };
  await api.sharedSearches.addAssociate(newSharedSearch.code, associationPayload);
  await api.sharedSearches.removeAssociate(newSharedSearch.code, newStoreAdmin.id);
  const associates = await api.sharedSearches.getAssociates(newSharedSearch.code);
  t.falsy(associates.find(associate => associate.id === newStoreAdmin.id));
});
