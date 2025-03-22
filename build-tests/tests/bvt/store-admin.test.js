import test from '../../helpers/test';
import { AdminApi } from '../../helpers/Api';
import isNumber from '../../helpers/isNumber';
import isString from '../../helpers/isString';
import isArray from '../../helpers/isArray';
import isDate from '../../helpers/isDate';
import $ from '../../payloads';

test('[bvt] Can list store admins', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const storeAdmins = await adminApi.storeAdmins.list();
  t.truthy(isArray(storeAdmins));
  t.truthy(storeAdmins.length >= 1);
  for (const storeAdmin of storeAdmins) {
    t.truthy(isNumber(storeAdmin.id));
    t.truthy(isString(storeAdmin.name));
    t.truthy(isString(storeAdmin.email));
    t.truthy(isString(storeAdmin.state));
    t.truthy(isString(storeAdmin.scope));
    t.truthy(isDate(storeAdmin.createdAt));
  }
});

test('[bvt] Can view store admin details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const storeAdmins = await adminApi.storeAdmins.list();
  const storeAdmin = await adminApi.storeAdmins.one(storeAdmins[0].id);
  t.truthy(isNumber(storeAdmin.id));
  t.truthy(isString(storeAdmin.name));
  t.truthy(isString(storeAdmin.email));
  t.truthy(isString(storeAdmin.state));
});

test('[bvt] Can create a new store admin', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomStoreAdminPayload();
  const newStoreAdmin = await adminApi.storeAdmins.create(payload);
  t.truthy(isNumber(newStoreAdmin.id));
  t.is(newStoreAdmin.state, 'invited');
  t.is(newStoreAdmin.name, payload.name);
  t.is(newStoreAdmin.email, payload.email);
});

test('[bvt] Can update store admin details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newStoreAdmin = await adminApi.storeAdmins.create($.randomStoreAdminPayload());
  const payload = $.randomStoreAdminPayload();
  const updatedStoreAdmin = await adminApi.storeAdmins.update(newStoreAdmin.id, payload);
  t.is(updatedStoreAdmin.id, newStoreAdmin.id);
  t.is(updatedStoreAdmin.state, newStoreAdmin.state);
  t.is(updatedStoreAdmin.name, payload.name);
  t.is(updatedStoreAdmin.email, payload.email);
});
