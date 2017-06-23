import { AdminApi } from '../helpers/Api';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('[bvt] Credit Card Token', function() {

	this.timeout(30000);

		it('[bvt] Can issue credit card token', async() => {
			const api = new AdminApi;
			await step.loginAsAdmin(api);
			const credentials = $.randomUserCredentials();
			const newCustomer = await step.createNewCustomer(api, credentials);
			const cardDetails = $.randomCreditCardDetailsPayload(newCustomer.id);
			const response = await step.getCreditCardToken(api, cardDetails);

			expect(response.token.constructor.name).to.exist;
			expect(response.brand.constructor.name).to.exist;
			expect(response.lastFour.constructor.name).to.exist;
			expect(response.token.length).to.equal(28);
			expect(response.lastFour.length).to.equal(4);
		});

});
