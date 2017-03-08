import test from '../helpers/test';
import Api from '../helpers/Api';
import $ from '../payloads';

test('Can issue credit card token', async (t) => {
  const api = await Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const response = await api.dev.creditCardToken($.randomCreditCardDetailsPayload(newCustomer.id));
  t.truthy(response.token.constructor.name, 'String');
  t.truthy(response.brand.constructor.name, 'String');
  t.truthy(response.lastFour.constructor.name, 'String');
  t.is(response.token.length, 28);
  t.is(response.lastFour.length, 4);
});
