'use strict';

const
  _       = require('underscore'),
  Chance  = require('chance');

const
  chance = new Chance();

class BaseModel {

  static generate() {
    let model = {id: chance.guid()};
    for (let key of this.seed) {
      let method = key.method || key.field;
      model[key.field] = chance[method](key.opts);
    }
    return new this(model);
  }

  static generateList(limit) {
    limit = limit || 50;
    let models = [];
    while(limit--) { models.push(this.generate()); }
    return models;
  }

  constructor(model) {
    this.model = model || {};
    if (!this.id) {
      this.amend(model);
      this.update({id: chance.guid()});
    }
  }

  get id() { return this.model.id; }
  get createdAt() { return this.model.createdAt.toISOString(); }

  update(values) {
    let proto = Object.getProtypeOf(this);
    for (let key in values) {
      let desc = Object.getOwnPropertyDescriptor(proto, key);
      if (desc && desc.set) this[key] = values[key];
    }
    return this;
  }

  amend(values) {
    if (this.whitelist) {
      values = _(values).pick(this.whitelist);
    }
    if (this.blacklist) {
      values = _(values).omit(this.blacklist);
    }
    return this.update(values);
  }
}

module.exports = BaseModel;
