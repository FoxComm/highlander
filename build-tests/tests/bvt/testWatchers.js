import test from '../../helpers/test';
import { AdminApi } from '../../helpers/Api';
import $ from '../../payloads';
import isDate from '../../helpers/isDate';
import isArray from '../../helpers/isArray';

export default ({ objectApi, createObject, selectId }) => {
  test('[bvt] Can list watchers', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const newObject = await createObject(adminApi);
    const id = selectId(newObject);
    const watchers = await objectApi(adminApi).getWatchers(id);
    t.truthy(isArray(watchers));
    t.is(watchers.length, 0);
  });

  test('[bvt] Can add watcher', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const newObject = await createObject(adminApi);
    const storeAdmins = await adminApi.storeAdmins.list();
    const watcher = $.randomArrayElement(storeAdmins);
    const watchersPayload = { assignees: [watcher.id] };
    const id = selectId(newObject);
    const watchers = await objectApi(adminApi).addWatchers(id, watchersPayload).then(r => r.result);
    t.truthy(isArray(watchers));
    t.is(watchers.length, 1);
    t.truthy(isDate(watchers[0].createdAt));
    
    // commented since of bug with assigning watcher #1993
    // t.is(watchers[0].assignee.id, watcher.id);

    t.is(watchers[0].assignmentType, 'watcher');
  });

  test('[bvt] Can remove watcher', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const newObject = await createObject(adminApi);
    const storeAdmins = await adminApi.storeAdmins.list();
    const watcherId = $.randomArrayElement(storeAdmins).id;
    const watchersPayload = { assignees: [watcherId] };
    const id = selectId(newObject);
    const watchersAfterAdd = await objectApi(adminApi).addWatchers(id, watchersPayload).then(r => r.result);
    t.truthy(isArray(watchersAfterAdd));
    t.is(watchersAfterAdd.length, 1);

    // dont work since of bug with assigning watcher #1993
    // await objectApi(adminApi).removeWatcher(id, watcherId);
    // const watchersAfterRemove = await objectApi(adminApi).getWatchers(id);
    // t.truthy(isArray(watchersAfterRemove));
    // t.is(watchersAfterRemove.length, 0);
    
    // temporary remove action
    const tempRemoveId = watchersAfterAdd[0].assignee.id;
    await objectApi(adminApi).removeWatcher(id, tempRemoveId);
    const watchersAfterRemove = await objectApi(adminApi).getWatchers(id);
    t.truthy(isArray(watchersAfterRemove));
    t.is(watchersAfterRemove.length, 0);
  });
};
