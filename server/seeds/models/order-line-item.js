'use strict';

const
  BaseModel = require('../lib/base-model');

const seed = [
  {field: 'name', method: 'word'},
  {field: 'orderId', method: 'integer', opts: {min: 1, max: 99999}},
  {field: 'sku', method: 'string'},
  {field: 'quantity', method: 'integer', opts: {min: 1, max: 19}},
  {field: 'status', method: 'pick', opts: ['Shipped', null, 'Hold']},
  {field: 'price', method: 'integer', opts: {min: 1000, max: 1000000}}
];

class OrderLineItem extends BaseModel {
  get name() { return this.model.name; }
  get orderId() { return this.model.orderId; }
  get sku() { return this.model.sku; }
  get status() { return this.model.status; }
  get price() { return this.model.price; }
  get quantity() { return this.model.quantity; }
  get total() { return this.model.quantity * this.model.price; }
  get imagePath() { return 'http://lorempixel.com/75/75/fashion'; }

  set quantity(val) { this.model.quantity = val; }
}

Object.defineProperty(OrderLineItem, 'seed', {value: seed});
Object.defineProperty(OrderLineItem, 'relationships', {value: ['order']});

module.exports = OrderLineItem;
