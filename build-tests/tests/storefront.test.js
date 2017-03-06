import superagent from 'superagent';
import test from '../helpers/test';
import startRandomUserSession from '../helpers/startRandomUserSession';
import Api from '../helpers/Api';
import $ from '../payloads';
import config from '../config';

for (const storefront of config.storefronts) {
  test(`Can access ${storefront.name} storefront`, async (t) => {
    const response = await superagent.get(storefront.url);
    t.is(response.status, 200);
  });

  test(`Can access ${storefront.name} login page`, async (t) => {
    const response = await superagent.get(`${storefront.url}/?auth=LOGIN`);
    t.is(response.status, 200);
  });

  test(`Can access ${storefront.name} sign up page`, async (t) => {
    const response = await superagent.get(`${storefront.url}/?auth=SIGNUP`);
    t.is(response.status, 200);
  });

  if (storefront.aboutPagePath) {
    test(`Can access ${storefront.name} "About Us" page`, async (t) => {
      const response = await superagent.get(`${storefront.url}/${storefront.aboutPagePath}`);
      t.is(response.status, 200);
    });
  }

  for (const category of storefront.categories) {
    test(`Can access ${storefront.name} category "${category}"`, async (t) => {
      const response = await superagent.get(`${storefront.url}/${encodeURIComponent(category)}`);
      t.is(response.status, 200);
    });
  }

  test(`Can access ${storefront.name} profile page`, async (t) => {
    const unauthorisedResponse = await superagent.get(`${storefront.url}/profile`);
    t.is(unauthorisedResponse.status, 200);
    t.is(unauthorisedResponse.redirects.length, 1);
    const customerApi = Api.withCookies();
    await startRandomUserSession(customerApi);
    const authorisedResponse = await customerApi.agent.get(`${storefront.url}/profile`);
    t.is(authorisedResponse.status, 200);
    t.is(authorisedResponse.redirects.length, 0);
  });

  test(`Can access ${storefront.name} product details page`, async (t) => {
    const adminApi = await Api.withCookies();
    await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
    const payload = $.randomProductPayload();
    const newProduct = await adminApi.products.create('default', payload);
    const customerApi = Api.withCookies();
    await startRandomUserSession(customerApi);
    const response = await customerApi.agent.get(`${storefront.url}/products/${newProduct.id}`);
    t.is(response.status, 200);
  });

  test(`Can search for products in ${storefront.name}`, async (t) => {
    const response = await superagent.get(`${storefront.url}/search/whatever`);
    t.is(response.status, 200);
  });
}
