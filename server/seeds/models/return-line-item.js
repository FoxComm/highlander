'use strict';

const
  BaseModel = require('../lib/base-model');

const seed = [
  {field: 'name', method: 'word'},
  {field: 'orderId', method: 'integer', opts: {min: 1, max: 99999}},
  {field: 'skuId', method: 'integer', opts: {min: 1, max: 99999}},
  {field: 'qty', method: 'integer', opts: {min: 1, max: 3}},
  {field: 'refund', method: 'integer', opts: {min: 1000, max: 10000}},
  {field: 'inventoryDisposition', method: 'pick', opts: ['Putaway']},
  {field: 'reason', method: 'pick', opts: ['Doesn\'t fit']},
  {field: 'imagePath', method: 'image', opts: {topic: 'technics'}}
];

class LineItem extends BaseModel {
  get name() { return this.model.name; }
  get orderId() { return this.model.orderId; }
  get skuId() { return this.model.skuId; }
  get price() { return this.model.price; }
  get qty() { return this.model.qty; }
  get inventoryDisposition() { return this.model.inventoryDisposition; }
  get reason() { return this.model.reason; }
  get refund() { return this.model.refund; }
  get imagePath() { return this.model.imagePath }
}

Object.defineProperty(LineItem, 'seed', {value: seed});

module.exports = LineItem;
