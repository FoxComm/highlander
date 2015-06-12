'use strict';

const
  _       = require('underscore'),
  errors  = require('../../errors');

class BaseModel {

  constructor(model) {
    this.model = model || {};
    if (!this.id) this.amend(model);
  }

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
