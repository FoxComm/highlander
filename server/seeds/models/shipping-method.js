'use strict';

const
 BaseModel = require('../lib/base-model');

const seed = [
  {field: 'name', method: 'pick', opts: ['Overnight', '2 Day Air', 'Standard 5-7 day Ground']},
  {field: 'price', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'isActive', method: 'bool', opts: {likelihood: 0}}
];

class ShippingMethod extends BaseModel {
  static generateList(limit) {
    let models = super.generateList(limit);
    for (let item of models) {
      if (item.id % 4 === 0) {
        item.isActive = true;
      }
    }
    return models;
  }
  get name() { return this.model.name; }
  get price() { return this.model.price; }
  get isActive() { return this.model.isActive; }

  set price(val) { this.model.price = +val; }
  set isActive(val) { this.model.isActive = val; }
}

Object.defineProperty(ShippingMethod, 'seed', {value: seed});

module.exports = ShippingMethod;
