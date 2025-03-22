import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('Shared Search', function() {

	this.timeout(30000);
	const newSharedSearchCodes = [];

	after('Remove shared searches created in tests', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		for (const code of newSharedSearchCodes) {
			await step.deleteSharedSearch(api, code);
		}
	});

	it('[bvt] Can list shared searches', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const sharedSearches = await step.listSharedSearch(api, 'ordersScope');
		expect(isArray(sharedSearches)).to.be.true;
		for (const sharedSearch of sharedSearches) {
			expect(isNumber(sharedSearch.id)).to.be.true;
			expect(isNumber(sharedSearch.storeAdminId)).to.be.true;
			expect(isString(sharedSearch.code)).to.be.true;
			expect(isString(sharedSearch.title)).to.be.true;
			expect(isString(sharedSearch.scope)).to.be.true;
			expect(isDate(sharedSearch.createdAt)).to.be.true;
			expect(sharedSearch.query).to.exist;
			expect(sharedSearch.rawQuery).to.exist;
		}
	});

	it('[bvt] Can create shared search', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(isString(newSharedSearch.code)).to.be.true;
		newSharedSearchCodes.push(newSharedSearch.code);
		expect(isNumber(newSharedSearch.id)).to.be.true;
		expect(isNumber(newSharedSearch.storeAdminId)).to.be.true;
		expect(isString(newSharedSearch.title)).to.be.true;
		expect(isString(newSharedSearch.scope)).to.be.true;
		expect(isDate(newSharedSearch.createdAt)).to.be.true;
		expect(newSharedSearch.query).to.exist;
		expect(newSharedSearch.rawQuery).to.exist;
	});

	it('[bvt] Can view shared search details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(isString(newSharedSearch.code)).to.be.true;
		newSharedSearchCodes.push(newSharedSearch.code);
		const foundSharedSearch = await step.getSharedSearch(api, newSharedSearch.code);
		expect(foundSharedSearch).to.deep.equal(newSharedSearch);
	});

	it('[bvt] Can delete shared search', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		await step.deleteSharedSearch(api, newSharedSearch.code);
		try {
			await step.getSharedSearch(api, newSharedSearch.code);
			expect('Shared search was found after deletion.').to.fail;
		} catch (error) {
			if (error && error.response) {
				expect(error.response.status).to.equal(404);
				expect(error.response.clientError).to.exist;
				expect(error.response.serverError).to.be.false;
			} else {
				throw error;
			}
		}
	});

	it('[bvt] Can list associates', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(isString(newSharedSearch.code)).to.be.true;
		newSharedSearchCodes.push(newSharedSearch.code);
		const associates = await step.getAssociates(api, newSharedSearch.code);
		expect(isArray(associates)).to.be.true;
		for (const associate of associates) {
			expect(isNumber(associate.id)).to.be.true;
			expect(isString(associate.email)).to.be.true;
			expect(isString(associate.name)).to.be.true;
			expect(isDate(associate.createdAt)).to.be.true;
		}
	});

	it('[bvt] Can add associate', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(isString(newSharedSearch.code)).to.be.true;
		newSharedSearchCodes.push(newSharedSearch.code);
		const newStoreAdmin = await step.createAdminUser(api, $.randomStoreAdminPayload());
		const associationPayload = { associates: [newStoreAdmin.id] };
		const updatedSharedSearch = await step
			.addAssociate(api, newSharedSearch.code, associationPayload)
			.then(r => r.result);
		expect(updatedSharedSearch).to.deep.equal(newSharedSearch);
		const associates = await step.getAssociates(api, newSharedSearch.code);
		expect(associates.length > 0).to.be.true;
		const newAssociate = associates.find(a => a.id === newStoreAdmin.id);
		expect(newAssociate.id).to.equal(newStoreAdmin.id);
		expect(newAssociate.name).to.equal(newStoreAdmin.name);
		expect(newAssociate.email).to.equal(newStoreAdmin.email);
	});

	it('[bvt] Can remove associate', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(isString(newSharedSearch.code)).to.be.true;
		newSharedSearchCodes.push(newSharedSearch.code);
		const newStoreAdmin = await step.createAdminUser(api, $.randomStoreAdminPayload());
		const associationPayload = { associates: [newStoreAdmin.id] };
		await step.addAssociate(api, newSharedSearch.code, associationPayload);
		await step.removeAssociate(api, newSharedSearch.code, newStoreAdmin.id);
		const associates = await step.getAssociates(api, newSharedSearch.code);
		expect(associates.find(associate => associate.id === newStoreAdmin.id)).to.not.exist;
	});

});
