import { CustomerApi, AdminApi } from '../helpers/Api';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import isDate from '../helpers/isDate';
import $ from '../payloads';
import { expect } from 'chai';
import * as step from '../helpers/steps';

describe('[bvt] Auth', function() {

	this.timeout(30000);

	it('[bvt] Can sign up', async () => {
		const api = new CustomerApi;
		const { email, name, password } = $.randomUserCredentials();
		const signupResponse = await step.signup(api, email, name, password);

		expect(isString(signupResponse.jwt)).to.be.true;
		expect(signupResponse.jwt.length > 0).to.be.true;
		expect(isNumber(signupResponse.user.id)).to.be.true;
		expect(isDate(signupResponse.user.createdAt)).to.be.true;
		expect(signupResponse.user.email).to.equal(email);
		expect(signupResponse.user.name).to.equal(name);
		expect(signupResponse.user.disabled).to.be.false;
		expect(signupResponse.user.isGuest).to.be.false;
		expect(signupResponse.user.isBlacklisted).to.be.false;
		expect(signupResponse.user.totalSales).to.equal(0);
		expect(signupResponse.user.storeCreditTotals.availableBalance).to.equal(0);
		expect(signupResponse.user.storeCreditTotals.currentBalance).to.equal(0);
	});

	it('[bvt] Can sign in as customer', async () => {
		const api = new CustomerApi;
		const { email, name, password } = $.randomUserCredentials();
		await step.signup(api, email, name, password);
		const loginResponse = await step.login(api, email, password, $.customerOrg);
		expect(loginResponse.jwt, 'Login response should have a "jwt" field.').to.exist;
		expect(loginResponse.user.name, 'Username in login response doesn\'t match real username.').to.equal(name);
		expect(loginResponse.user.email, 'Email in login response doesn\'t match real user email.').to.equal(email);
	});

	it('[bvt] Can sign in as admin', async () => {
		const api = new AdminApi;
		const loginResponse = await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
		expect(loginResponse.jwt, 'Login response should have a "jwt" field.').to.exist;
		expect(loginResponse.user && loginResponse.user.name, 'Login response should have an "user.name" field.').to.exist;
		expect(loginResponse.user && loginResponse.user.email, 'Login response should have an "user.email" field.').to.exist;
	});


	it('[bvt] Can\'t sign in as admin with a customer org', async () => {
		const api = new AdminApi;
		try {
			await step.login(api, $.adminEmail, $.adminPassword, $.customerOrg);
			expect('Signing in as admin with a customer org should have failed, but it succeeded.').to.be.false;
		} catch (error) {
			if (error && error.response) {
				expect(error.response.status).to.equal(400);
				expect(error.response.clientError).to.be.true;
				expect(error.response.serverError).to.be.false;
			} else {
				throw error;
			}
		}
	});

	it('[bvt] Can sign out', async () => {
		const api = new CustomerApi;
		await step.login(api, $.adminEmail, $.adminPassword, $.adminOrg);
		await step.logout(api)
	});

	it('[bvt] Can view customer account details', async () => {
		const api = new CustomerApi;
		const { email, name, password } = $.randomUserCredentials();
		const signupResponse = await step.signup(api, email, name, password);
		const foundAccount = await step.getAccount(api);
		expect(foundAccount).to.deep.equal(signupResponse.user);
	});

});
