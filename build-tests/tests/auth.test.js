import faker from 'faker';
import test from '../helpers/test';
import Api from '../helpers/Api';
import $ from '../payloads';

test('Can sign up', async () => {
  const api = Api.withoutCookies();
  const email = faker.internet.email();
  const name = faker.name.firstName();
  const password = faker.internet.password();
  await api.auth.signup(email, name, password);
});

test('Can sign in as customer', async (t) => {
  const api = Api.withoutCookies();
  const email = faker.internet.email();
  const name = faker.name.firstName();
  const password = faker.internet.password();
  await api.auth.signup(email, name, password);
  const loginResponse = await api.auth.login(email, password, $.customerOrg);
  t.truthy(loginResponse.jwt, 'Login response should have a "jwt" field.');
  t.is(loginResponse.user.name, name, 'Username in login response doesn\'t match real username.');
  t.is(loginResponse.user.email, email.toLowerCase(), 'Email in login response doesn\'t match real user email.');
});

test('Can sign in as admin', async (t) => {
  const api = Api.withoutCookies();
  const loginResponse = await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  t.truthy(loginResponse.jwt, 'Login response should have a "jwt" field.');
  t.truthy(loginResponse.user && loginResponse.user.name, 'Login response should have an "user.name" field.');
  t.truthy(loginResponse.user && loginResponse.user.email, 'Login response should have an "user.email" field.');
});

test('Can\'t sign in as admin with a customer org', async (t) => {
  const api = Api.withoutCookies();
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

test('Can sign out', async () => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  await api.auth.logout();
});
