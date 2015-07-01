'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer');

const seed = [
  {field: 'body', method: 'paragraph', opts: {sentences: 3}},
  {field: 'isEditable', method: 'bool'}
];

class Note extends BaseModel {
  get body() { return this.model.body; }
  get author() { return Customer.findOne(this.model.customerId); }
  get isEditable() { return this.model.isEditable; }

  set isEditable(value) { this.model.isEditable = value; }
  set orderId(id) { this.model.customerId = +id; }
  set customerId(id) { this.model.customerId = +id; }
}

Object.defineProperty(Note, 'seed', {value: seed});
Object.defineProperty(Note, 'relationships', {value: ['order', 'customer']});

module.exports = Note;
