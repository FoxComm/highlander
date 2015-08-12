'use strict';

const
  BaseModel = require('../lib/base-model'),
  errors    = require('../../errors');

const seed = [
  {field: 'referenceNumber', method: 'string', opts: {length: 8, pool: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'}},
  {field: 'orderNumber', method: 'string', opts: {length: 8, pool: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'}},
  {field: 'returnStatus', method: 'pick', opts: ['Processing', 'Fulfilled', 'Declined']},
  {field: 'asignee', method: 'pick', opts: ['Unassigned']},
  {field: 'returnTotal', method: 'integer', opts: {min: 100, max: 10000}}
];

class Return extends BaseModel {

  static findByIdOrRef(id) {
    let results = this.data.filter(function(item) {
      return item.id === +id || item.referenceNumber === id;
    });
    if (!results.length) { throw new errors.NotFound(`Cannot find ${this.name}`); }
    return new this(results[0]);
  }

  get referenceNumber() { return this.model.referenceNumber; }
  get returnStatus() { return this.model.returnStatus; }
  get totals() {
    return {
      total: this.model.refundTotal
    };
  }

  set returnStatus(status) { this.model.returnStatus = status; }
}

Object.defineProperty(Return, 'seed', {value: seed});
Object.defineProperty(Return, 'relationships', {value: ['order']});

module.exports = Return;
