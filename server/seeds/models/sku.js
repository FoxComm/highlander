'use strict';

const
  BaseModel = require('../lib/base-model');

const seed = [
  { field: 'name', method: 'word'},
  { field: 'skuId', method: 'integer', opts: {min: 1, max: 99999}},
  { field: 'price', method: 'integer', opts: {min: 1000, max: 1000000}}
];

class Sku extends BaseModel {
  get name() { return this.model.name; }
  get skuId() { return this.model.skuId; }
  get price() { return this.model.price; }
  get image() { return 'http://lorempixel.com/75/75/fashion'; }
}

Object.defineProperty(Sku, 'seed', {value: seed});

module.exports = Sku;
