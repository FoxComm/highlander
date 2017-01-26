const _ = require('lodash');
const { faker}  = require('faker');
const Nightmare = require('nightmare');
const system = require('system');

var home = "https://hal.foxcommerce.com";

async function homepage(c) {
    return c.page.goto(home);
}

async function signup(c) {
  console.log("state signup");
}

async function category(c) {

  var cat = _.sample(c.args['select']);

  var url = home + '/' + cat;
  console.log("state category: " + url);

  return c.page
    .goto(url)
    .evaluate(() => { 
      return document.querySelector('#product');
    }).then((products) => {
      console.log("PRODUCTS: " + products);
    });
}

async function product(c) {
  console.log("state product");
}

async function cart(c) {
  console.log("state cart");
}

async function purchase(c) {
  console.log("state purchase");
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

