'use strict';

import _ from 'lodash';
import Api from './api';
import EventedStore from './evented-store';

export default class BaseStore extends EventedStore {
  constructor() {
    super();
    this.models = [];
  }

  uri(id) {
    return id ? `${this.baseUri}/${id}` : this.baseUri;
  }

  notifyChanged() {
    this.dispatch('change', this.models);
  }

  reset() {
    this.models = [];
    this.notifyChanged();
  }

  sort(field, order) {
    this.models = this._sort(this.models, field, order);
    this.notifyChanged();
  }

  getState(id) {
    if (!id) return this.models;
    return this.findModel(id);
  }

  findModel(id) {
    return this._findModel(this.models, id);
  }

  findWhere(...args) {
    return this._findWhere(this.models, ...args);
  }

  upsert(model) {
    this._upsert(this.models, model);
  }

  add(model) {
    this.models.push(model);
    this.notifyChanged();
  }

  update(model) {
    this._update(this.models, model);
    this.notifyChanged();

    if (!_.isArray(model)) {
      this.dispatch('changeItem', model);
    }
  }

  updateBehaviour(res) {
    this.update(res);
  }

  deleteBehaviour(res, id) {
    if (this._delete(this.models, id)) {
      this.notifyChanged();
    }
  }

  patch(id, changes) {
    return super.patch(changes, id);
  }
}
