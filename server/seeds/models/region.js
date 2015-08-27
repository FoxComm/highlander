'use strict';

const BaseModel = require('../lib/base-model');

const seed = [
  {field: 'code', method: 'state'},
  {field: 'name', method: 'state', opts: {full: true}}
];

class Region extends BaseModel {
  get code() { return this.model.code; }
  get name() { return this.model.name; }
}

Object.defineProperty(Region, 'seed', {value: seed});

module.exports = Region;
