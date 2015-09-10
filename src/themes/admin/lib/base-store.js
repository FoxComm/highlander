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
    this.models = this.models.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
    this.notifyChanged();
  }

  getState(id) {
    if (!id) return this.models;
    return this.findModel(id);
  }

  findModel(id) {
    return _.find(this.models, 'id', id);
  }

  findWhere(...args) {
    return _.findWhere(this.models, ...args);
  }

  process(model) {
    return model;
  }

  upsert(model) {
    let exists = this.findModel(model.id);
    if (exists) {
      let idx = this.models.indexOf(exists);
      if (idx !== -1) {
        this.models[idx] = this.process(model) || model;
      }
    } else {
      this.models.push(this.process(model) || model);
    }
  }

  add(model) {
    this.models.push(model);
    this.notifyChanged();
  }

  update(model) {
    if (Array.isArray(model)) {
      model.forEach((item) => {
        this.upsert(item);
      });
      this.notifyChanged();
    } else {
      this.upsert(model);
      this.dispatch('changeItem', model);
    }
  }

  fetch(id) {
    return Api.get(this.uri(id))
      .then((res) => { this.update(res); return res; })
      .catch((err) => { this.apiError(err); return err; });
  }

  patch(id, changes) {
    return Api.patch(this.uri(id), changes)
      .then((res) => { this.update(res); return res; })
      .catch((err) => { this.apiError(err); return err; });
  }

  create(data) {
    return Api.post(this.uri(), data)
      .then((res) => { this.update(res); return res; })
      .catch((err) => { this.apiError(err); return err; });
  }
}
