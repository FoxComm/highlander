const _ = require('lodash');
const faker  = require('faker');
const system = require('system');
const fox = require('api-js');
const superagent = require('superagent');
const countries = require('./countries.json');

function createApi(apiBaseUrl ,stripeKey, options = {}) {

  if (!apiBaseUrl) {
    throw new Error('API_URL is not defined in process.env');
  }

  let agent = superagent;

  if (options.preserveCookies) {
    agent = superagent.agent();
  }

  return new fox.default({
    api_url: apiBaseUrl + '/api',
    stripe_key: stripeKey,
    agent,
    handleResponse: options.handleResponse,
  });
}

function setup(c) {
  const stripe = require('stripe')(c.stripeKey);

  c.stripe = stripe;
  c.api = createApi(c.home, c.stripeKey);
}

async function homepage(c) {
  console.log("api homepage");
}

async function signup(c) {
  console.log("api signup");
  const email = faker.internet.email();
  const name = faker.name.firstName();
  const password = faker.internet.password();

  c.name = name;
  let customer = await c.api.auth.signup(email, name, password).catch(err => console.log('error:', err));
  c.customer = customer;
  c.api.addAuth(customer.jwt);
  let cart = await c.api.cart.get();
  c.cart = cart;

}

async function category(c) {
  let cat = _.sample(c.args['select']);

  let query = {"query":{"bool":{"filter":[{"term":{"context":"default"}},{"term":{"tags": cat}}]}}};
  let products = await c.api.get(`/search/public/products_catalog_view/_search?size=1000`);

  _.forEach(products.result, async (product) => {
    await c.api.analytics.trackEvent({
        channel: 1,
        subject: 1,
        verb: 'list',
        obj: 'product',
        objId: product.id,
    });
  });

  c.product = _.sample(products.result);
  console.log("PROD: " + c.product.id);
}

async function product(c) {
  console.log("api product");
  return c.api.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'pdp',
      obj: 'product',
      objId: c.product.id,
  });
}

async function cart(c) {
  console.log("api cart");
  return c.api.cart.addSku(c.product.skus[0], 1);
}

function getRegion(state) {
  var r = 0;
  _.forEach(countries.regions, (region) => {
    if(region.name === state) r = region.id;
  });
  return r;
}

async function purchase(c) {
  console.log("api purchase");
  let cart = await c.api.cart.get();
  c.cart = cart;

  await c.api.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'checkout',
      obj: 'cart',
      objId: cart.referenceNumber,
  });
  _.map(cart.skus, async sku => {
      const productId = _.get(sku, 'productFormId', null);
      await c.api.analytics.trackEvent({
          channel: 1,
          subject: 1,
          verb: 'checkout',
          obj: 'product',
          objId: productId,
      });
  });

  let fcard = faker.helpers.createCard();
  let address = {
    address1: fcard.address.streetA,
    address2: "",
    city: fcard.address.city,
    name: c.name,
    phoneNumber: "6666666666",
    zip: fcard.address.zipcode.substring(0, 5),
    regionId: getRegion(fcard.address.state),
    isDefault: false,
    country: 'United States'
  };

  let addr = await c.api.addresses.add(address);
  await c.api.cart.setShippingAddressById(addr.id);

  let shippingMethods = await c.api.cart.getShippingMethods();
  await c.api.cart.chooseShippingMethod(_.sample(shippingMethods).id);

  let creditCard = {
    holderName: c.name,
    number: '4242424242424242',
    cvc: '123',
    expMonth: '01',
    expYear: '2023',
    address1: address.address1,
    address2: address.address2,
    zip: address.zip,
    city: address.city,
    state: address.state,
    country: address.country
  }

  let token = await c.stripe.tokens.create({
        card: {
          "name": c.name,
          "number": '4242424242424242',
          "exp_month": 12,
          "exp_year": 2018,
          "cvc": '123'
        }
  });
  let card = await c.api.creditCards.createCardFromStripeToken(token, address, false);
  await c.api.cart.addCreditCard(card.id);

  await c.api.cart.checkout();

  await c.api.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'purchase',
      obj: 'order',
      objId: cart.referenceNumber,
  });
  _.map(cart.skus, async sku => {
      const productId = _.get(sku, 'productFormId', null);
      await c.api.analytics.trackEvent({
          channel: 1,
          subject: 1,
          verb: 'purchase',
          obj: 'product',
          objId: productId,
      });
      foxApi.analytics.trackEvent({
          channel: 1,
          subject: 1,
          verb: 'purchase-quantity',
          obj: 'product',
          count: sku.quantity,
          objId: productId
      });
      foxApi.analytics.trackEvent({
          channel: 1,
          subject: 1,
          verb: 'revenue',
          obj: 'product',
          count: sku.price,
          objId: productId,
      });
  });
}

async function clear_cart(c) {
  console.log("api clear_cart");
}

const stateFunctions = {
  homepage: homepage,
  signup: signup,
  category: category,
  product: product,
  cart: cart,
  purchase: purchase,
  clear_cart: clear_cart
};

exports.setup = setup;
exports.stateFunctions = stateFunctions;

