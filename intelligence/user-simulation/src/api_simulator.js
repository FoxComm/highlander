const _ = require('lodash');
const faker  = require('faker');
const system = require('system');
const fox = require('api-js');
const superagent = require('superagent');
const countries = require('./countries.json');
const SHA1 = require('crypto-js/sha1');

function productHash(productId) {
  return SHA1(`products/${productId}`).toString();
}

function cartHash(cartRefNum) {
  return SHA1(`carts/${cartRefNum}`).toString();
}

function orderHash(cartRefNum) {
  return SHA1(`orders/${cartRefNum}`).toString();
}

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
}

async function signup(c) {
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

  if(_.isNil(c.category)) 
    c.category = _.sample(c.args['select']);
  else if(_.random(1.0, true) > 0.90)  //once in a while switch categories
    c.category = _.sample(c.args['select']);

  let query = {
    "query":{ 
      "function_score" : { 
        "query": { 
          "bool": { 
            "filter":[{"term":{"context":"default"}}], 
            "must": [{
              "nested": {
                "path": "taxonomies", 
                "query":{
                  "bool" : {
                    "must": [{
                      "query":{
                        "bool": {
                          "should": { "term": {"taxonomies.taxons": c.category} }
                        }
                      }
                    }]
                  }
                }
              }
            }]
          }
        }, "random_score": {}
      } 
    }
  };

  let products = await c.api.get(`/search/public/products_catalog_view/_search?size=100`,
  query);

  _.forEach(products.result, async (product) => {
    await c.api.analytics.trackEvent({
        channel: 1,
        subject: 1,
        verb: 'list',
        obj: 'product',
        objId: productHash(product.productId),
    });
  });

  c.product = _.sample(products.result);
  console.log("PROD: " + c.product.productId);
}

async function product(c) {
  await c.api.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'pdp',
      obj: 'product',
      objId: productHash(c.product.productId),
  });
}

function addToLineItems(items, sku, quantity, attributes) {
  return items.concat([{ sku, quantity, attributes }]);
}

async function cart(c) {
  let cart = await c.api.cart.get();

  const skus = cart.lineItems.skus;
  const newSku = _.sample(c.product.skus);
  console.log("\tCART: " + newSku);

  const newLineItems = addToLineItems(skus, newSku, 1, {});
  await c.api.post('/v1/my/cart/line-items', newLineItems);

  await c.api.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'cart',
      obj: 'product',
      objId: productHash(c.product.productId),
  });
}

function getRegion(state) {
  var r = 0;
  _.forEach(countries.regions, (region) => {
    if(region.name === state) r = region.id;
  });
  return r;
}

async function purchase(c) {
  let cart = await c.api.cart.get();
  c.cart = cart;

  await c.api.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'checkout',
      obj: 'cart',
      objId: cartHash(cart.referenceNumber),
  });
  _.forEach(cart.lineItems.skus, async sku => {
      const productId = sku.productFormId;
      console.log("\tSKU: " + sku.sku);
      await c.api.analytics.trackEvent({
          channel: 1,
          subject: 1,
          verb: 'checkout',
          obj: 'product',
          count: sku.quantity,
          objId: productHash(productId),
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
}

async function clear_cart(c) {
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
