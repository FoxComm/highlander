'use strict';

const
  BaseModel = require('../lib/base-model');

const whitelist = [
  'name',
  'email'
];

const seed = [
  {field: 'firstName', method: 'first'},
  {field: 'lastName', method: 'last'},
  {field: 'email'},
  {field: 'phone'},
  {field: 'address'},
  {field: 'city'},
  {field: 'state'},
  {field: 'zip'},
  {field: 'role', method: 'pick', opts: ['admin', 'customer', 'vendor']},
  {field: 'blocked', method: 'bool'},
  {field: 'cause', method: 'sentence', opts: {words: 2}},
  {field: 'createdAt', method: 'date', opts: {year: 2014}}
];

class Customer extends BaseModel {

  get firstName() { return this.model.firstName; }
  get lastName() { return this.model.lastName; }
  get email() { return this.model.email; }
  get phone() { return this.model.phone; }
  get address() { return this.model.address; }
  get city() { return this.model.city; }
  get state() { return this.model.state; }
  get zip() { return this.model.zip; }
  get role() { return this.model.role; }
  get blocked() { return this.model.blocked; }
  get cause() { return this.model.cause; }

  toJSON() {
    return {
      id: this.id,
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      phone: this.phone,
      address: this.address,
      city: this.city,
      state: this.state,
      zip: this.zip,
      role: this.role,
      blocked: this.blocked,
      cause: this.cause,
      createdAt: this.createdAt
    };
  }
}

Object.defineProperty(Customer.prototype, 'whitelist', {value: whitelist});
Object.defineProperty(Customer, 'seed', {value: seed});

module.exports = Customer;
