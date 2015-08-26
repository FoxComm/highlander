'use strict';

const
  BaseModel = require('../lib/base-model'),
  Customer  = require('./customer'),
  Address   = require('./address'),
  LineItem  = require('./line-item'),
  Note      = require('./note');

const events = {
  itemAdded: {name: 'Item Added', next: LineItem},
  itemEdited: {name: 'Item Edited', prev: LineItem, next: LineItem},
  discountAdded: {name: 'Discount Added'},
  shippingEdited: {name: 'Shipping Address Edited', prev: Address, next: Address},
  remorseAdded: {name: '15 Min Added to Remorse Hold'},
  noteCreated: {name: 'Note Created', next: Note, field: 'body'},
  statusChanged: {name: 'Order Status Changed', prev: LineItem, next: LineItem, field: 'status'},
  viewed: {name: 'Order Viewed'}
};

const eventTypes = [
  'itemAdded',
  'itemEdited',
  'discountAdded',
  'shippingEdited',
  'remorseAdded',
  'noteCreated',
  'statusChanged',
  'viewed'
];

const seed = [
  {field: 'eventType', method: 'pick', opts: eventTypes}
];

class Activity extends BaseModel {
  event() {
    let type = this.model.eventType;
    return events[type];
  }

  getPrevious() {
    let
      event = this.event(),
      prev  = event.prev ? event.prev.generate() : null;
    if (!prev) return null;
    return event.field ? prev[event.field] : prev;
  }

  getNext() {
    let
      event = this.event(),
      next  = event.next ? event.next.generate() : null;
    if (!next) return null;
    return event.field ? next[event.field] : next;
  }

  get eventType() { return this.model.eventType; }
  get eventName() { return this.event().name; }
  get orderId() { return this.model.orderId; }
  get user() { return Customer.findOne(this.model.customerId); }
  get previousState() { return this.getPrevious(); }
  get newState() { return this.getNext(); }

  set orderId(id) { this.model.customerId = +id; }
  set customerId(id) { this.model.customerId = +id; }
  set giftCardId(id) { this.model.giftCardId = +id; }
}

Object.defineProperty(Activity, 'seed', {value: seed});
Object.defineProperty(Activity, 'relationships', {value: ['order', 'customer', 'giftCard']});

module.exports = Activity;
