'use strict';

const
  BaseModel = require('../lib/base-model');

const seed = [
  {field: 'cardNumber', method: 'integer', opts: {min: 1000000000000000, max: 9999999999999999}},
  {field: 'type', method: 'pick', opts: ['Appeasement', 'Customer Purchase', 'Marketing']},
  {field: 'balance', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'availableBalance', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'currentBalance', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'state', method: 'pick', opts: ['Active', 'On Hold', 'Canceled']},
  {field: 'date', method: 'date', opts: {year: 2014}}
];

class GiftCard extends BaseModel {
  get cardNumber() { return this.model.cardNumber; }
  get type() { return this.model.type; }
  get balance() { return this.model.balance; }
  get state() { return this.model.state; }
  get date() { return this.model.date; }
  get availableBalance() { return this.model.availableBalance; }
  get currentBalance() { return this.model.currentBalance; }
}

Object.defineProperty(GiftCard, 'seed', {value: seed});

module.exports = GiftCard;
