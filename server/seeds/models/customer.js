'use strict';

const
  BaseModel = require('../lib/base-model');

const whitelist = [
  'name',
  'email'
]

class Customer extends BaseModel {
  get name() { return this.model.name; }
  get email() { return this.model.email; }

  toJSON() {
    return {
      name: this.name,
      email: this.email
    };
  }
}

Object.defineProperty(Customer.prototype, 'whitelist', {value: whitelist});

module.exports = Customer;
