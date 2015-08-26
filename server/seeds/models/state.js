'use strict';

const BaseModel = require('../lib/base-model');

const seed = [
  {field: 'abbreviation', method: 'state'},
  {field: 'name', method: 'state', opts: {full: true}}
];

class State extends BaseModel {
  get abbreviation() { return this.model.abbreviation; }
  get name() { return this.model.name; }
}

Object.defineProperty(State, 'seed', {value: seed});

module.exports = State;
