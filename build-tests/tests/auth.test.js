import test from '../helpers/test';
import { CustomerApi, AdminApi } from '../helpers/Api';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import isDate from '../helpers/isDate';
import $ from '../payloads';

test('Can sign up', async (t) => {
  const api = new CustomerApi(t);
  const { email, name, password } = $.randomUserCredentials();
  const signupResponse = await api.auth.signup(email, name, password);
  t.truthy(isString(signupResponse.jwt));
  t.truthy(signupResponse.jwt.length > 0);
  t.truthy(isNumber(signupResponse.user.id));
  t.truthy(isDate(signupResponse.user.createdAt));
  t.is(signupResponse.user.email, email);
  t.is(signupResponse.user.name, name);
  t.is(signupResponse.user.disabled, false);
  t.is(signupResponse.user.isGuest, false);
  t.is(signupResponse.user.isBlacklisted, false);
  t.is(signupResponse.user.totalSales, 0);
  t.is(signupResponse.user.storeCreditTotals.availableBalance, 0);
  t.is(signupResponse.user.storeCreditTotals.currentBalance, 0);
});

test('Can sign in as customer', async (t) => {
  const api = new CustomerApi(t);
  const { email, name, password } = $.randomUserCredentials();
  await api.auth.signup(email, name, password);
  const loginResponse = await api.auth.login(email, password, $.customerOrg);
  t.truthy(loginResponse.jwt, 'Login response should have a "jwt" field.');
  t.is(loginResponse.user.name, name, 'Username in login response doesn\'t match real username.');
  t.is(loginResponse.user.email, email, 'Email in login response doesn\'t match real user email.');
});

test('Can sign in as admin', async (t) => {
  const api = new AdminApi(t);
  const loginResponse = await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  t.truthy(loginResponse.jwt, 'Login response should have a "jwt" field.');
  t.truthy(loginResponse.user && loginResponse.user.name, 'Login response should have an "user.name" field.');
  t.truthy(loginResponse.user && loginResponse.user.email, 'Login response should have an "user.email" field.');
});

test('Can\'t sign in as admin with a customer org', async (t) => {
  const api = new AdminApi(t);
  try {
    await api.auth.login($.adminEmail, $.adminPassword, $.customerOrg);
    t.fail('Signing in as admin with a customer org should have failed, but it succeeded.');
  } catch (error) {
    if (error && error.response) {
      t.is(error.response.status, 400);
      t.truthy(error.response.clientError);
      t.falsy(error.response.serverError);
    } else {
      throw error;
    }
  }
});

test('Can sign out', async (t) => {
  const api = new CustomerApi(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  await api.auth.logout();
});

test('Can view customer account details', async (t) => {
  const api = new CustomerApi(t);
  const { email, name, password } = $.randomUserCredentials();
  const signupResponse = await api.auth.signup(email, name, password);
  const foundAccount = await api.account.get();
  t.deepEqual(foundAccount, signupResponse.user);
});
