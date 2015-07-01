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
  static generateList(limit) {
    let models = super.generateList(limit);
    for (let item of models) {
      if (item.id % 7 === 0) {
        item.isDefault = true;
        item.isActive = true;
      }
    }
    return models;
  }

  static defaultForCustomer(id) {
    let
      addresses       = this.findByCustomer(id),
      defaultAddress  = addresses.filter(function(a) { return a.isDefault; });
    if (!defaultAddress.length) return null;
    return defaultAddress[0];
  }

  get isDefault() { return this.model.isDefault; }
  get isActive() { return this.model.isActive; }
  get state() { return this.model.state; }
  get name() { return this.model.name; }
  get street1() { return this.model.street1; }
  get street2() {
    if (this.model.street2) {
      return this.model.street2;
    }
    if (chance.bool({likelihood: 30})) {
      return chance.address();
    } else {
      return null;
    }
  }
  get city() { return this.model.city; }
  get zip() { return this.model.zip; }
  get country() { return this.model.country; }
  get customer() { return Customer.findOne(this.model.customerId); }

  set isDefault(val) { this.model.isDefault = val; }
  set isActive(val) { this.model.isActive = val; }
  set customerId(id) { this.model.customerId = +id; }
}

Object.defineProperty(Address, 'seed', {value: seed});
Object.defineProperty(Address, 'relationships', {value: ['customer']});

module.exports = Address;
