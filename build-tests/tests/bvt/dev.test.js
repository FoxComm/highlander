import test from '../../helpers/test';
import { AdminApi } from '../../helpers/Api';
import $ from '../../payloads';

test('[bvt] Can issue credit card token', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const credentials = $.randomUserCredentials();
  const newCustomer = await adminApi.customers.create(credentials);
  const response = await adminApi.dev.creditCardToken($.randomCreditCardDetailsPayload(newCustomer.id));
  t.truthy(response.token.constructor.name, 'String');
  t.truthy(response.brand.constructor.name, 'String');
  t.truthy(response.lastFour.constructor.name, 'String');
  t.is(response.token.length, 28);
  t.is(response.lastFour.length, 4);
});
