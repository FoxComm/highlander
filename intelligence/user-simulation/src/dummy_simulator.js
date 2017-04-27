const _ = require('lodash');
const faker  = require('faker');
const system = require('system');

function setup(c) {
  console.log("dummy setup");
}

async function homepage(c) {
  console.log("dummy homepage");
}

async function signup(c) {
  console.log("dummy signup");
}

async function category(c) {
  console.log("dummy category");
}

async function product(c) {
  console.log("dummy product");
}

async function cart(c) {
  console.log("dummy cart");
}

async function purchase(c) {
  console.log("dummy purchase");

}

async function clear_cart(c) {
  console.log("dummy clear_cart");
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

