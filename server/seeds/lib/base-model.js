'use strict';

const
  _       = require('underscore'),
  Chance  = require('chance'),
  errors  = require('../../errors');

const
  chance = new Chance();

class BaseModel {

  static generate(id) {
    let model = {
      id: id || chance.integer({min: 1001, max: 999999}),
      createdAt: chance.date({year: 2014})
    };

    for (let key of this.seed) {
      let method = key.method || key.field;
      model[key.field] = chance[method](key.opts);
    }
    return new this(model);
  }

  static generateList(limit) {
    limit = limit || 50;
    let models = [];
    while(limit--) { models.push(this.generate(limit)); }
    return models;
  }

  static findOne(id) {
    id = +id;
    let result = this.data.filter(function(item) {
      return item.id === id;
    });
    if (!result.length) { throw new errors.NotFound(`Cannot find ${this.name}`); }
    return new this(result[0].model);
  }

  static paginate(limit, page) {
    limit = +limit || 50;
    page = page ? +page - 1 : 0;
    return this.data.slice(limit * page, (limit * page) + limit);
  }

  constructor(model) {
    this.model = model || {};
    if (!this.id) {
      this.model.id = chance.guid();
      this.model.createdAt = chance.date({year: 2014});
      this.amend(model);
    }
  }

  get id() { return this.model.id; }
  get createdAt() { return this.model.createdAt.toISOString(); }

  update(values) {
    let proto = Object.getPrototypeOf(this);
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

  toJSON() {
    let
      proto = Object.getPrototypeOf(this),
      names = Object.getOwnPropertyNames(proto);

    let json = {
      id: this.id,
      createdAt: this.createdAt
    };

    for (let key of names) {
      let desc = Object.getOwnPropertyDescriptor(proto, key);
      if (desc && desc.get) json[key] = this[key];
    }
    return json;
  }
}

module.exports = BaseModel;
