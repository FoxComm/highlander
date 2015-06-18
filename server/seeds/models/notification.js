'use strict';

const
  BaseModel = require('../lib/base-model'),
  Order = require('./order');

const seed = [
  {field: 'notificationStatus', method: 'pick', opts: ['Delivered', 'Failed']},
  {field: 'subject', method: 'pick', opts: ['Shipment Confirmation', 'Review Your Items', 'Order Confirmation']},
  {field: 'sendDate', method: 'date', opts: {year: 2014}},
  {field: 'contact', method: 'pick', opts: ['jim@bob.com', '+ (567) 203 - 8430']}
];

class Notification extends BaseModel {
  get notificationStatus() { return this.model.notificationStatus; }
  get subject() { return this.model.subject; }
  get sendDate() { return this.model.sendDate; }
  get contact() { return this.model.contact; }

  toJSON() {
    return {
      id: this.id,
      order: Order.generate(),
      notificationStatus: this.notificationStatus,
      subject: this.subject,
      sendDate: this.sendDate,
      contact: this.contact
    };
  }
}

Object.defineProperty(Notification, 'seed', {value: seed});

module.exports = Notification;
