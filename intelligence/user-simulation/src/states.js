const _ = require('lodash');
const faker  = require('faker');
const Nightmare = require('nightmare');
const system = require('system');

var home = "https://hal.foxcommerce.com";

async function homepage(c) {
    return c.page.goto(home);
}

async function signup(c) {
  let signupPage = home + '/?auth=SIGNUP';
  let name = faker.name.firstName() + ' ' + faker.name.lastName();
  c.name = name;

  let email = faker.internet.email();
  let pass = faker.internet.password();
  return c.page.goto(signupPage).refresh().
    insert('input#signup-username', name).
    insert('input#signup-email', email).
    insert('input#signup-password', pass).
    wait('button#signup-submit').
    click('button#signup-submit').
    wait(1000);
}

async function category(c) {

  let cat = _.sample(c.args['select']);

  let url = home + '/' + cat;

  return c.page.goto(url);
}

async function product(c) {
  return c.page
    .evaluate((prob) => { 
      let products = document.getElementById('products-list').childNodes;
      let clicked = false;
      while(!clicked && products.length > 0) {
        for(let i = 0; i < products.length; i++)
        {
          if(Math.random() <= prob) {
            products[i].click();
            clicked = true;
            break;
          }
        }
      }
    }, 0.1);
}

async function cart(c) {
  return c.page.
    wait(1000).
    wait("button#add-to-cart").
    click("button#add-to-cart").
    wait(1000);
}

async function purchase(c) {
  let checkoutPage = home + '/checkout';
  let card = faker.helpers.createCard();
  if(!_.isNil(c.alreadyPurchased)) {
    return c.page.
      wait('#cart-checkout').
      click('#cart-checkout').
      wait(1000).
      goto(checkoutPage).
        wait('input[name="delivery"]').
        click('input[name="delivery"]').
        wait('#delivery-method-submit').
        click('#delivery-method-submit').
        wait('input[name="credit-card"]').
        wait('#payment-method-submit').
        click('#payment-method-submit');
  } else {
    c.alreadyPurchased = true;
    return c.page.
      click('#cart-checkout').
      wait(1000).
      goto(checkoutPage).
      wait('input[name="address1"]').
      insert('input[name="name"]', c.name).
      insert('input[name="address1"]', card.address.streetA).
      type('input[name="zip"]', card.address.zipcode.substring(0, 5)).
      type('input[name="city"]', card.address.city).
      click('input[name="phone-number"]').
      type('input[name="phone-number"]', '6666666666').
      click('input[name="phone-number"]').
      type('input[name="phone-number"]', '6666666666').
      wait('#add-address-submit').
      click('#add-address-submit').
      wait('label[for="address-radio-0"]').
      wait('#shipping-address-submit').
      click('#shipping-address-submit').
      wait('input[name="delivery"]').
      click('input[name="delivery"]').
      wait('#delivery-method-submit').
      click('#delivery-method-submit').
      wait(1000).
      wait('#billing-add-card').
      click('#billing-add-card').
      wait('input[name="holderName"]').
      type('input[name="holderName"]', c.name).
      type('input[name="billing-card-number"]', '4242424242424242').
      type('input[name="billing-cvc', '123').
      type('input[name="billing-month', '01').
      type('input[name="billing-year', '2020').
      wait('#add-card-submit').
      click('#add-card-submit').
      wait('#payment-method-submit').
      click('#payment-method-submit');
  }

}

async function clear_cart(c) {
  console.log("state clear_cart");
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

exports.stateFunctions = stateFunctions;

