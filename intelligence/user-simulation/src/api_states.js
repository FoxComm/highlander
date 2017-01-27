const _ = require('lodash');
const faker  = require('faker');
const system = require('system');

async function homepage(c) {
  console.log("api homepage");
}

async function signup(c) {
  console.log("api signup");
}

async function category(c) {
  console.log("api category");
}

async function product(c) {
  console.log("api product");
}

async function cart(c) {
  console.log("api cart");
}

async function purchase(c) {
  console.log("api purchase");

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

exports.stateFunctions = stateFunctions;

