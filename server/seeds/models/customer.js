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
  {field: 'modality', method: 'pick', opts: ['iPhone', 'Android', 'Web']},
  {field: 'disabled', method: 'bool', opts: {likelihood: 30}},
  {field: 'cause', method: 'sentence', opts: {words: 2}},
  {field: 'isLocker', method: 'bool', opts: {likelihood: 30}},
  {field: 'isGuest', method: 'bool', opts: {likelihood: 10}},
  {field: 'rankLevel', method: 'integer', opts: {min: 0, max: 100}},
  {field: 'groups', method: 'sentence', opts: {words: 4}}
];

class Customer extends BaseModel {

  get firstName() { return this.model.firstName; }
  get lastName() { return this.model.lastName; }
  get email() { return this.model.email; }
  get phoneNumber() { return this.model.phone; }
  get address() { return this.model.address; }
  get city() { return this.model.city; }
  get state() { return this.model.state; }
  get zip() { return this.model.zip; }
  get role() { return this.model.role; }
  get location() { return `${this.city}, ${this.state}`; }
  get disabled() { return this.model.disabled; }
  get cause() { return this.model.cause; }
  get modality() { return this.model.modality; }
  get isLocker() { return this.model.isLocker; }
  get isGuest() { return this.model.isGuest; }
  get rank() { return `Top ${this.model.rankLevel}%`; }
  get groups() { return this.model.groups.split(/\s+/)}
  get avatarUrl() { return 'http://lorempixel.com/84/84/people/'; }
}

Object.defineProperty(Customer.prototype, 'whitelist', {value: whitelist});
Object.defineProperty(Customer, 'seed', {value: seed});

module.exports = Customer;
