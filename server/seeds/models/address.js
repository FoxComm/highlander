'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer'),
  Chance  = require('chance');

const seed = [
  {field: 'createdAt', method: 'date', opts: {year: 2014}},
  {field: 'isDefault', method: 'bool', opts: {likelihood: 0}},
  {field: 'isActive', method: 'bool', opts: {likelihood: 0}},
  {field: 'state'},
  {field: 'name'},
  {field: 'street1', method: 'address'},
  {field: 'city'},
  {field: 'zip'},
  {field: 'country'}
];

const
  chance = new Chance();

class Address extends BaseModel {
  get isDefault() { return this.model.isDefault; }
  set isDefault(val) { this.model.isDefault = val; }
  get isActive() { return this.model.isActive; }
  set isActive(val) { this.model.isActive = val; }
  get state() { return this.model.state; }
  get name() { return this.model.name; }
  get street1() { return this.model.street1; }
  get street2() {
    if (chance.bool({likelihood: 30})) {
      return chance.address();
    } else {
      return null;
    }
  }
  get city() { return this.model.city; }
  get zip() { return this.model.zip; }
  get country() { return this.model.country; }
  get customer() { return Customer.generate(); }

  static generateList(limit) {
    let models = super.generateList(limit);
    models[0].update({
      isDefault: true,
      isActive: true
    });
    console.log(models);
    return models;
  }
}

Object.defineProperty(Address, 'seed', {value: seed});

module.exports = Address;
