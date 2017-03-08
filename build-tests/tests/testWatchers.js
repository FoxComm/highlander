import test from '../helpers/test';
import Api from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';

export default ({ objectApi, createObject, selectId }) => {
  test('Can list watchers', async (t) => {
    const api = Api.withCookies(t);
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(api);
    const id = selectId(newObject);
    const watchers = await objectApi(api).getWatchers(id);
    t.truthy(isArray(watchers));
    t.is(watchers.length, 0);
  });

  test('Can add watcher', async (t) => {
    const api = Api.withCookies(t);
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(api);
    const storeAdmins = await api.storeAdmins.list();
    const watcher = $.randomArrayElement(storeAdmins);
    const watchersPayload = { assignees: [watcher.id] };
    const id = selectId(newObject);
    const watchers = await objectApi(api).addWatchers(id, watchersPayload).then(r => r.result);
    t.truthy(isArray(watchers));
    t.is(watchers.length, 1);
    t.truthy(isDate(watchers[0].createdAt));
    t.is(watchers[0].assignee.id, watcher.id);
    t.is(watchers[0].assignmentType, 'watcher');
  });

  test('Can remove watcher', async (t) => {
    const api = Api.withCookies(t);
    await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const newObject = await createObject(api);
    const storeAdmins = await api.storeAdmins.list();
    const watcherId = $.randomArrayElement(storeAdmins).id;
    const watchersPayload = { assignees: [watcherId] };
    const id = selectId(newObject);
    const watchersAfterAdd = await objectApi(api).addWatchers(id, watchersPayload).then(r => r.result);
    t.truthy(isArray(watchersAfterAdd));
    t.is(watchersAfterAdd.length, 1);
    await objectApi(api).removeWatcher(id, watcherId);
    const watchersAfterRemove = await objectApi(api).getWatchers(id);
    t.truthy(isArray(watchersAfterRemove));
    t.is(watchersAfterRemove.length, 0);
  });
};
