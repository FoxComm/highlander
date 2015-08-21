'use strict';

const
  BaseModel = require('../lib/base-model');

const seed = [
  {field: 'cardType', method: 'pick', opts: ['visa', 'mastercard', 'discover', 'amex']},
  {field: 'cardExp', method: 'exp'},
  {field: 'cardNumber', method: 'integer', opts: {min: 1000000000000000, max: 9999999999999999}},
  {field: 'amount', method: 'integer', opts: {min: 1000, max: 1000000}},
  {field: 'status', method: 'pick', opts: ['Auth', 'Decline']}
];

class Payment extends BaseModel {
  get method() {
    let
      cardNumber  = this.model.cardNumber.toString(),
      lastFour    = cardNumber.replace(/.*(\d{4})$/, 'xxxx xxxx xxxx $1');

    return {
      cardType: this.model.cardType,
      cardExp: this.model.cardExp,
      cardNumber: lastFour
    };
  }
  get amount() { return this.model.amount; }
  get status() { return this.model.status; }
}

Object.defineProperty(Payment, 'seed', {value: seed});
Object.defineProperty(Payment, 'relationships', {value: ['order']});

module.exports = Payment;
