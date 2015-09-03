'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer'),
  Address   = require('./address'),
  LineItem  = require('./line-item'),
  Payment   = require('./payment'),
  moment    = require('moment'),
  errors    = require('../../errors');

const seed = [
  {field: 'referenceNumber', method: 'string', opts: {length: 8, pool: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'}},
  {field: 'orderStatus', method: 'pick', opts: ['cart', 'fraudHold', 'remorseHold', 'manualHold', 'canceled',
                                                'fulfillmentStarted', 'partiallyShipped', 'shipped']},
  {field: 'paymentStatus', method: 'pick', opts: ['Partial Capture', 'Full Capture']},
  {field: 'shippingStatus', method: 'pick', opts: ['New', null]},
  {field: 'fraudScore', method: 'integer', opts: {min: 0, max: 100}},
  {field: 'shippingTotal', method: 'integer', opts: {min: 0, max: 500}},
  {field: 'tax', method: 'integer', opts: {min: 100, max: 10000}},
  {field: 'discount', method: 'integer', opts: {min: 0, max: 1000}},
  {field: 'subtotal', method: 'integer', opts: {min: 10000, max: 1000000}},
  {field: 'total', method: 'integer', opts: {min: 10000, max: 1000000}},
  {field: 'email', method: 'email'}
];

class Order extends BaseModel {
  static findByIdOrRef(id) {
    let results = this.data.filter(function(item) {
      return item.id === +id || item.referenceNumber === id;
    });
    if (!results.length) { throw new errors.NotFound(`Cannot find ${this.name}`); }
    return new this(results[0]);
  }

  get referenceNumber() { return this.model.referenceNumber; }
  get orderStatus() { return this.model.orderStatus; }
  get paymentStatus() { return this.model.paymentStatus; }
  get shippingStatus() { return this.model.shippingStatus; }
  get fraudScore() { return this.model.fraudScore; }
  get customer() { return Customer.findOne(this.model.customerId); }
  get shippingAddress() { return Address.defaultForCustomer(this.model.customerId); }
  get lineItems() { return LineItem.generateList(~~((Math.random() * 5) + 1)); }
  get payments() { return Payment.findByOrder(this.id); }
  get totals() {
    return {
      shipping: this.model.shippingTotal,
      subTotal: this.model.subtotal,
      taxes: this.model.tax,
      adjustments: this.model.discount,
      total: this.model.total
    };
  }
  get email() { return this.model.email; }
  get total() { return this.model.total; }
  get remorseEnd() { return moment.utc().add(3, 'h').format(); }

  set orderStatus(status) { this.model.orderStatus = status; }
  set customerId(id) { this.model.customerId = +id; }

  viewers() {
    let
      limit = ~~((Math.random() * 5) + 2),
      page  = ~~(Math.random() * 15);
    return Customer.paginate(limit, page);
  }
}

Object.defineProperty(Order, 'seed', {value: seed});
Object.defineProperty(Order, 'relationships', {value: ['customer']});

module.exports = Order;
