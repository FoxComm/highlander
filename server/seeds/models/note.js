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
  get author() { return Customer.generate(); }
  get isEditable() { return this.model.isEditable; }
}

Object.defineProperty(Note, 'seed', {value: seed});

module.exports = Note;
