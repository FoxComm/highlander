'use strict';

const
  BaseModel = require('../lib/base-model'),
  GiftCard  = require('./gift-card'),
  Order     = require('./order');

const seed = [
  {field: 'amount', method: 'integer', opts: {min: -10000, max: 10000}},
  {field: 'state', method: 'pick', opts: ['Authorized', 'Captured', 'New Gift Card']},
  {field: 'availableBalance', method: 'integer', opts: {min: 0, max: 10000}}
];

class GiftCardTransaction extends BaseModel {
  get amount() { return this.model.amount; }
  get state() { return this.model.state; }
  get availableBalance() { return this.model.availableBalance; }
  get order() { return Order.findOne(this.model.orderId); }
  get giftCard() { return GiftCard.findOne(this.model.giftCardId); }
  get orderRef() { return this.order.referenceNumber; }

  set orderId(val) { this.model.orderId = +val; }
  set giftCardId(val) { this.model.giftCardId = +val; }
}

Object.defineProperty(GiftCardTransaction, 'seed', {value: seed});
Object.defineProperty(GiftCardTransaction, 'relationships', {value: ['order', 'giftCard']});

module.exports = GiftCardTransaction;
