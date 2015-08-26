'use strict';

const
  _       = require('lodash'),
  Chance  = require('chance'),
  errors  = require('../../errors');

const
  chance = new Chance();

class BaseModel {

  static nextId() {
    let
      ids     = this.data.map(function(i) { return i.id; }),
      nextId  = ids.length ? Math.max.apply(null, ids) + 1 : 1;
    return nextId;
  }

  static generate() {
    let model = {
      id: this.nextId(),
      createdAt: chance.date({year: 2014})
    };

    for (let key of this.seed) {
      let method = key.method || key.field;
      model[key.field] = chance[method](key.opts);
    }
    this.data.push(model);
    return model;
  }

  static generateList(limit) {
    limit = limit || 50;
    let models = [];
    while(limit--) { models.push(this.generate()); }
    return models;
  }

  static filterById(field, id) {
    id = +id;
    return _.filter(this.data, function(item) {
      return item[field] === id;
    });
  }

  static filterByQuery(query) {
    let
      filter  = query.q.split(':'),
      field   = filter[0],
      regex   = new RegExp(filter[1], 'i');
    return _.filter(this.data, function(item) {
      return regex.test(item[field]);
    });
  }

  static find(field, id) {
    return id ? this.filterById(field, id) : this.filterByQuery(field);
  }

  static findOne(id) {
    let
      Model = this,
      result = _.find(this.data, function(item) {
        return item.id === +id;
      });
    if (!result) { throw new errors.NotFound(`Cannot find ${this.name}`); }
    return new Model(result);
  }

  static findAll(field, id) {
    let
      Model = this,
      results = this.find(field, id);
    return results.map(function(i) { return new Model(i); });
  }

  static findByCustomer(customerId) {
    return this.findAll('customerId', customerId);
  }

  static findByOrder(orderId) {
    return this.findAll('orderId', orderId);
  }

  static findIndex(id) {
    return this.data.map(function (item) { return item.id; }).indexOf(id);
  }

  static deleteOne(id) {
    let index = this.findIndex(id);
    if (index > -1) {
      this.data.splice(index, 1);
      return true;
    }
    return false;
  }

  static paginate(limit, page) {
    limit = +limit || 50;
    page = page ? +page - 1 : 0;
    return this.data.slice(limit * page, (limit * page) + limit);
  }

  static upsert(model) {
    let idx = this.findIndex(model.id);
    if (idx > -1) {
      this.data[idx] = model;
    } else {
      this.data.push(model);
    }
  }

  static update(model) {
    if (Array.isArray(model)) {
      for (let item of model) {
        this.upsert(item);
      }
    } else {
      this.upsert(model);
    }
  }

  constructor(model) {
    this.model = model || {};
    if (!this.id) {
      let newModel = this.constructor.generate();
      this.model.id = newModel.id;
      this.model.createdAt = newModel.createdAt;
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
    this.constructor.update(this.model);
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
