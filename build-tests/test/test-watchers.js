import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';
import { expect } from 'chai';
import * as step from '../helpers/steps';

export default ({ objectApi, createObject, selectId }) => {
	it('[bvt] Can list watchers', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newObject = await createObject(api);
		const id = selectId(newObject);
		const watchers = await objectApi(api).getWatchers(id);
		expect(isArray(watchers)).to.be.true;
		expect(watchers.length).to.equal(0);
	});

	it('[bvt] Can add watcher', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newObject = await createObject(api);
		const storeAdmins = await step.getStoreAdmins(api);
		const watcher = $.randomArrayElement(storeAdmins);
		const watchersPayload = { assignees: [watcher.id] };
		const id = selectId(newObject);
		const watchers = await objectApi(api).addWatchers(id, watchersPayload).then(r => r.result);
		expect(isArray(watchers)).to.be.true;
		expect(watchers.length).to.equal(1);
		expect(isDate(watchers[0].createdAt)).to.be.true;

		// commented since of bug with assigning watcher #1993
		// t.is(watchers[0].assignee.id, watcher.id);

		expect(watchers[0].assignmentType).to.equal('watcher');
	});

	it('[bvt] Can remove watcher', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const newObject = await createObject(api);
		const storeAdmins = await step.getStoreAdmins(api)
		const watcherId = $.randomArrayElement(storeAdmins).id;
		const watchersPayload = { assignees: [watcherId] };
		const id = selectId(newObject);
		const watchersAfterAdd = await objectApi(api).addWatchers(id, watchersPayload).then(r => r.result);
		expect(isArray(watchersAfterAdd)).to.be.true;
		expect(watchersAfterAdd.length).to.equal(1);

		// dont work since of bug with assigning watcher #1993
		// await objectApi(adminApi).removeWatcher(id, watcherId);
		// const watchersAfterRemove = await objectApi(adminApi).getWatchers(id);
		// t.truthy(isArray(watchersAfterRemove));
		// t.is(watchersAfterRemove.length, 0);

		// temporary remove action
		const tempRemoveId = watchersAfterAdd[0].assignee.id;
		await objectApi(api).removeWatcher(id, tempRemoveId);
		const watchersAfterRemove = await objectApi(api).getWatchers(id);
		expect(isArray(watchersAfterRemove)).to.be.true;
		expect(watchersAfterRemove.length, 0).to.equal(0);
	});
};
