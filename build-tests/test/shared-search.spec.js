import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate'
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
		expect(sharedSearches).to.be.a('array');
		for (const sharedSearch of sharedSearches) {
			expect(sharedSearch.id).to.be.a('number');
			expect(sharedSearch.storeAdminId).to.be.a('number');
			expect(sharedSearch.code).to.be.a('string');
			expect(sharedSearch.title).to.be.a('string');
			expect(sharedSearch.scope).to.be.a('string');
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
		expect(newSharedSearch.code).to.be.a('string');
		newSharedSearchCodes.push(newSharedSearch.code);
		expect(newSharedSearch.id).to.be.a('number');
		expect(newSharedSearch.storeAdminId).to.be.a('number');
		expect(newSharedSearch.title).to.be.a('string');
		expect(newSharedSearch.scope).to.be.a('string');
		expect(isDate(newSharedSearch.createdAt)).to.be.true;
		expect(newSharedSearch.query).to.exist;
		expect(newSharedSearch.rawQuery).to.exist;
	});

	it('[bvt] Can view shared search details', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(newSharedSearch.code).to.be.a('string');
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
		expect(newSharedSearch.code).to.be.a('string');
		newSharedSearchCodes.push(newSharedSearch.code);
		const associates = await step.getAssociates(api, newSharedSearch.code);
		expect(associates).to.be.a('array');
		for (const associate of associates) {
			expect(associate.id).to.be.a('number');
			expect(associate.email).to.be.a('string');
			expect(associate.name).to.be.a('string');
			expect(isDate(associate.createdAt)).to.be.true;
		}
	});

	it('[bvt] Can add associate', async () => {
		const api = new AdminApi;
		await step.loginAsAdmin(api);
		const payload = $.randomSharedSearchPayload();
		const newSharedSearch = await step.createNewSharedSearch(api, payload);
		expect(newSharedSearch.code).to.be.a('string');
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
		expect(newSharedSearch.code).to.be.a('string');
		newSharedSearchCodes.push(newSharedSearch.code);
		const newStoreAdmin = await step.createAdminUser(api, $.randomStoreAdminPayload());
		const associationPayload = { associates: [newStoreAdmin.id] };
		await step.addAssociate(api, newSharedSearch.code, associationPayload);
		await step.removeAssociate(api, newSharedSearch.code, newStoreAdmin.id);
		const associates = await step.getAssociates(api, newSharedSearch.code);
		expect(associates.find(associate => associate.id === newStoreAdmin.id)).to.not.exist;
	});

});
