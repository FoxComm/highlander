'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer');

const seed = [
  {field: 'orderId', method: 'string', opts: {length: 15, pool: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'}},
  {field: 'createdAt', method: 'date', opts: {year: 2014}},
  {field: 'orderStatus', method: 'pick', opts: ['Shipped', 'Fullfillment Started', 'Remorse Hold']},
  {field: 'paymentStatus', method: 'pick', opts: ['Partial Capture', 'Full Capture']},
  {field: 'shippingStatus', method: 'pick', opts: ['New', null]},
  {field: 'total', method: 'integer', opts: {min: 10000, max: 1000000}}
];

class Order extends BaseModel {
  get orderId() { return this.model.orderId; }
  get orderStatus() { return this.model.orderStatus; }
  get paymentStatus() { return this.model.paymentStatus; }
  get shippingStatus() { return this.model.shippingStatus; }
  get customer() { return Customer.generate(); }
  get total() { return this.model.total; }
}

Object.defineProperty(Order, 'seed', {value: seed});

module.exports = Order;
