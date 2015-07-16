'use strict';

const
 BaseModel = require('../lib/base-model');

const seed = [
  {field: 'name', method: 'pick', opts: ['Overnight', '2 Day Air', 'Standard 5-7 day Ground']},
  {field: 'price', method: 'integer', opts: {min: 1000, max: 10000}}
];

class ShippingMethod extends BaseModel {
  get name() { return this.model.name; }
  get price() { return this.model.price; }

  set price(val) { this.model.price = +val; }
}

Object.defineProperty(ShippingMethod, 'seed', {value: seed});

module.exports = ShippingMethod;
