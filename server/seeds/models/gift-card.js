'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer');

const seed = [
  {field: 'code', method: 'integer', opts: {min: 1000000000000000, max: 9999999999999999}},
  {field: 'type', method: 'pick', opts: ['Appeasement', 'Customer Purchase', 'Marketing']},
  {field: 'originalBalance', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'availableBalance', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'currentBalance', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'status', method: 'pick', opts: ['Active', 'On Hold', 'Canceled']},
  {field: 'date', method: 'date', opts: {year: 2014}},
  {field: 'message', method: 'paragraph'}
];

class GiftCard extends BaseModel {
  get code() { return this.model.code; }
  get type() { return this.model.type; }
  get subType() { return this.model.subType; }
  get originalBalance() { return this.model.originalBalance; }
  get status() { return this.model.status; }
  get date() { return this.model.date; }
  get availableBalance() { return this.model.availableBalance; }
  get currentBalance() { return this.model.currentBalance; }
  get customer() {
    if (this.model.customerId) {
      return Customer.findOne(this.model.customerId);
    } else {
      return undefined;
    }
  }
  get recipient() {
    if (this.model.recipientId) {
      return Customer.findOne(this.model.recipientId);
    } else {
      return undefined;
    }
  }
  get message() { return this.model.message; }

  set type(val) { this.model.type = val; }
  set subType(val) { this.model.subType = val; }
  set originalBalance(val) { this.model.originalBalance = val; }
  set currentBalance(val) { this.model.currentBalance = val; }
  set availableBalance(val) { this.model.availableBalance = val; }
  set status(val) { this.model.status = val; }
  set date(val) { this.model.date = val; }

  set customerId(val) { this.model.customerId = val; }
}

Object.defineProperty(GiftCard, 'seed', {value: seed});
Object.defineProperty(GiftCard, 'relationships', {value: ['customer']});

module.exports = GiftCard;
