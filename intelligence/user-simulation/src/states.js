const _ = require('lodash');
const faker  = require('faker');
const Nightmare = require('nightmare');
const system = require('system');

var home = "https://hal.foxcommerce.com";

async function homepage(c) {
    return c.page.goto(home);
}

async function signup(c) {
  console.log("state signup");
  var signupPage = home + '/?auth=SIGNUP';
  return c.page.goto(signupPage)
    type("input#signup-username", faker.name.firstName() + ' ' + faker.name.lastName()).
    type("input#signup-email", faker.internet.email()).
    type("input#signup-password", faker.internet.password()).
    wait(5000).
    click("button#signup-submit").
    wait(5000);
}

async function category(c) {

  var cat = _.sample(c.args['select']);

  var url = home + '/' + cat;
  console.log("state category: " + url);

  return c.page.goto(url);//.refresh();
}

async function product(c) {
  console.log("state product");
  return c.page
    .evaluate((prob) => { 
      var products = document.getElementById('products-list').childNodes;
      var clicked = false;
      while(!clicked && products.length > 0) {
        for(var i = 0; i < products.length; i++)
        {
          if(Math.random() < prob) {
            products[i].click();
            clicked = true;
            break;
          }
        }
      }
    }, 0.3);
}

async function cart(c) {
  console.log("state cart");
  return c.page.
    wait(1000).
    click("button#add-to-cart").
    wait(1000);
}

async function purchase(c) {
  console.log('state purchase');
  return c.page.
    click('#cart-checkout').
    click('#delivery2').
    click('#delivery-method-submit').
    click('#payment-method-submit')
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

