import { AdminApi, CustomerApi } from '../helpers/Api';
import $ from '../payloads';
import config from '../config';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('Storefront', function() {

	this.timeout(30000);

	for (const storefront of config.storefronts) {
		it(`[bvt] Can access ${storefront.name} storefront`, async () => {
			const response = await step.getPage(storefront.url);
			expect(response.status).to.equal(200);
		});

		it(`[bvt] Can access ${storefront.name} login page`, async () => {
			const response = await step.getPage(`${storefront.url}/?auth=LOGIN`);
			expect(response.status).to.equal(200);
		});

		it(`[bvt] Can access ${storefront.name} sign up page`, async () => {
			const response = await step.getPage(`${storefront.url}/?auth=SIGNUP`);
			expect(response.status).to.equal(200);
		});

		if (storefront.aboutPagePath) {
			it(`[bvt] Can access ${storefront.name} "About Us" page`, async () => {
				const response = await step.getPage(`${storefront.url}/${storefront.aboutPagePath}`);
				expect(response.status).to.equal(200);
			});
		}

		for (const category of storefront.categories) {
			it(`[bvt] Can access ${storefront.name} category "${category}"`, async () => {
				const response = await step.getPage(`${storefront.url}/${encodeURIComponent(category)}`);
				expect(response.status).to.equal(200);
			});
		}

		it(`Can access ${storefront.name} profile page`, async () => {
			const unauthorisedResponse = await step.getPage(`${storefront.url}/profile`);
			expect(unauthorisedResponse.status).to.equal(200);
			expect(unauthorisedResponse.redirects.length).to.equal(1);
			const customerApi = new CustomerApi;
			await step.loginAsCustomer(customerApi);
			const authorisedResponse = await step.userGetPage(customerApi, `${storefront.url}/profile`);
			expect(authorisedResponse.status).to.equal(200);
			expect(authorisedResponse.redirects.length).to.equal(0);
		});

		it(`Can access ${storefront.name} product details page`, async () => {
			const api = new AdminApi;
			await step.loginAsAdmin(api);
			const payload = $.randomProductPayload();
			const newProduct = await step.createNewProduct(api, 'default', payload);
			const customerApi = new CustomerApi;
			await step.loginAsCustomer(customerApi);
			const response = await step.userGetPage(customerApi, `${storefront.url}/products/${newProduct.id}`);
			expect(response.status).to.equal(200);
		});

		it(`Can search for products in ${storefront.name}`, async () => {
			const response = await step.getPage(`${storefront.url}/search/whatever`);
			expect(response.status).to.equal(200);
		});
	}
});


