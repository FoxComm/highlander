import test from 'ava';
import faker from 'faker';
import Api from '../helpers/Api';

const ADMIN_EMAIL = 'admin@admin.com';
const ADMIN_PASSWORD = 'password';
const ADMIN_ORG = 'tenant';
const CUSTOMER_ORG = 'merchant';

test('Can sign up', async (t) => {
  const api = Api.withoutCookies();
  const email = faker.internet.email();
  const name = faker.name.firstName();
  const password = faker.internet.password();
  try {
    await api.auth.signup(email, name, password);
  } catch (error) {
    t.ifError(error);
  }
});

test('Can sign in as customer', async (t) => {
  const api = Api.withoutCookies();
  const email = faker.internet.email();
  const name = faker.name.firstName();
  const password = faker.internet.password();
  try {
    await api.auth.signup(email, name, password);
    const loginResponse = await api.auth.login(email, password, CUSTOMER_ORG);
    t.truthy(loginResponse.jwt, 'Login response should have a "jwt" field.');
    t.is(loginResponse.user.name, name, 'Username in login response doesn\'t match real username.');
    t.is(loginResponse.user.email, email.toLowerCase(), 'Email in login response doesn\'t match real user email.');
  } catch (error) {
    t.ifError(error);
  }
});

test('Can sign in as admin', async (t) => {
  const api = Api.withoutCookies();
  try {
    const loginResponse = await api.auth.login(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_ORG);
    t.truthy(loginResponse.jwt, 'Login response should have a "jwt" field.');
    t.truthy(loginResponse.user && loginResponse.user.name, 'Login response should have an "user.name" field.');
    t.truthy(loginResponse.user && loginResponse.user.email, 'Login response should have an "user.email" field.');
  } catch (error) {
    t.ifError(error);
  }
});

test('Can\'t sign in as admin with a customer org', async (t) => {
  const api = Api.withoutCookies();
  try {
    await api.auth.login(ADMIN_EMAIL, ADMIN_PASSWORD, CUSTOMER_ORG);
    t.fail('Signing in as admin with a customer org should have failed, but it succeeded.');
  } catch (error) {
    t.truthy(error);
    t.truthy(error.response);
    t.is(error.response.status, 400);
    t.truthy(error.response.clientError);
    t.falsy(error.response.serverError);
  }
});

test('Can sign out', async (t) => {
  const api = Api.withCookies();
  try {
    await api.auth.login(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_ORG);
    await api.auth.logout();
  } catch (error) {
    t.ifError(error);
  }
});
