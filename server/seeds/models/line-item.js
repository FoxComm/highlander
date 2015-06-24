'use strict';

const
  BaseModel = require('../lib/base-model');

const seed = [
  {field: 'name', method: 'word'},
  {field: 'orderId', method: 'integer', opts: {min: 1, max: 99999}},
  {field: 'skuId', method: 'integer', opts: {min: 1, max: 99999}},
  {field: 'quantity', method: 'integer', opts: {min: 1, max: 19}},
  {field: 'status', method: 'pick', opts: ['Shipped', null, 'Hold']},
  {field: 'price', method: 'integer', opts: {min: 1000, max: 1000000}}
];

class LineItem extends BaseModel {
  get name() { return this.model.name; }
  get orderId() { return this.model.orderId; }
  get skuId() { return this.model.skuId; }
  get status() { return this.model.status; }
  get price() { return this.model.price; }
  get quantity() { return this.model.quantity; }
  get total() { return this.model.quantity * this.model.price; }
  get image() { return 'http://lorempixel.com/75/75/fashion'; }
}

Object.defineProperty(LineItem, 'seed', {value: seed});

module.exports = LineItem;
