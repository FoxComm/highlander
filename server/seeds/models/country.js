'use strict';

const BaseModel = require('../lib/base-model');

const seed = [
  {field: 'code', method: 'country'},
  {field: 'name', method: 'country', opts: {full: true}}
];

class Country extends BaseModel {
  get code() { return this.model.code; }
  get name() { return this.model.name; }
}

Object.defineProperty(Country, 'seed', {value: seed});

module.exports = Country;
