'use strict';

const BaseModel = require('../lib/base-model');
const LineItem  = require('./return-line-item');
const Order = require('./order');
const Customer = require('./customer');
const errors = require('../../errors');

const seed = [
  {field: 'referenceNumber', method: 'string', opts: {length: 8, pool: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'}},
  {field: 'email', method: 'pick', opts: ['bree@foxcommerce.com']},
  {field: 'orderNumber', method: 'pick', opts: [Order.data[0].referenceNumber]},
  {field: 'returnStatus', method: 'pick', opts: ['Pending', 'Processing', 'Complete']},
  {field: 'returnType', method: 'pick', opts: ['Standard Return']},
  {field: 'assignee', method: 'pick', opts: ['Unassigned']},
  {field: 'customerMessage', method: 'paragraph'},
  {field: 'shipping', method: 'integer', opts: {min: 20, max: 80}},
  {field: 'taxes', method: 'integer', opts: {min: 5, max: 15}},
  {field: 'subtotal', method: 'integer', opts: {min: 500, max: 10000}}
];

class Return extends BaseModel {
  static findByIdOrRef(id) {
    let results = this.data.filter(function(item) {
      return item.id === +id || item.referenceNumber === id;
    });
    if (!results.length) {
      throw new errors.NotFound(`Cannot find ${this.name}`);
    }
    return new this(results[0]);
  }

  get referenceNumber() { return this.model.referenceNumber; }
  get email() { return this.model.email; }
  get orderNumber() { return this.model.orderNumber; }
  get returnStatus() { return this.model.returnStatus; }
  get returnType() { return this.model.returnType; }
  get assignee() { return this.model.assignee; }
  set customerId(id) { this.model.customerId = +id; }
  get customer() { return Customer.findOne(this.model.customerId); }
  get customerMessage() { return this.model.customerMessage; }
  get lineItems() { return LineItem.generateList(~~((Math.random() * 5) + 1)); }
  get totals() {
    return {
      shipping: this.model.shipping,
      subtotal: this.model.subtotal,
      taxes: this.model.taxes,
      total: this.model.subtotal + this.model.shipping + this.model.taxes
    };
  }

  set returnStatus(status) { this.model.returnStatus = status; }

  viewers() {
    let limit = ~~((Math.random() * 5) + 2);
    let page = ~~(Math.random() * 15);
    return Customer.paginate(limit, page);
  }
}

Object.defineProperty(Return, 'seed', {value: seed});
Object.defineProperty(Return, 'relationships', {value: ['order', 'customer']});

module.exports = Return;
