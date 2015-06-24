'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer'),
  Note      = require('./note'),
  Notification = require('./notification');

const seed = [
  {field: 'orderId', method: 'string', opts: {length: 8, pool: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'}},
  {field: 'orderStatus', method: 'pick', opts: ['cart', 'fraudHold', 'remorseHold', 'manualHold', 'canceled', 'fulfillmentHold', 'partiallyShipped', 'shipped']},
  {field: 'paymentStatus', method: 'pick', opts: ['Partial Capture', 'Full Capture']},
  {field: 'shippingStatus', method: 'pick', opts: ['New', null]},
  {field: 'fraudScore', method: 'integer', opts: {min: 0, max: 100}},
  {field: 'shippingTotal', method: 'integer', opts: {min: 0, max: 500}},
  {field: 'subtotal', method: 'integer', opts: {min: 10000, max: 1000000}},
  {field: 'grandTotal', method: 'integer', opts: {min: 10000, max: 1000000}}
];

class Order extends BaseModel {
  get orderId() { return this.model.orderId; }
  get orderStatus() { return this.model.orderStatus; }
  set orderStatus(status) { this.model.orderStatus = status; }
  get paymentStatus() { return this.model.paymentStatus; }
  get shippingStatus() { return this.model.shippingStatus; }
  get fraudScore() { return this.model.fraudScore; }
  get customer() { return Customer.generate(); }
  get shippingTotal() { return this.model.shippingTotal; }
  get subtotal() { return this.model.subtotal; }
  get grandTotal() { return this.model.grandTotal; }

  viewers() {
    let
      count = ~~((Math.random() * 5) + 2),
      users = [];
    while(count--) users.push(Customer.generate());
    return users;
  }

  notes() {
    let
      count = ~~((Math.random() * 5) + 0),
      notes = [];
    while(count--) notes.push(Note.generate());
    return notes;
  }

  notifications() {
    let
      count = ~~((Math.random() * 10) + 0),
      notifications = [];
    while(count--) notifications.push(Notification.generate());
    return notifications;
  }
}

Object.defineProperty(Order, 'seed', {value: seed});

module.exports = Order;
