'use strict';

import Api from './api';
import { dispatch, listenTo, stopListeningTo } from './dispatcher';
import { inflect } from 'fleck';

export default class BaseStore {
  constructor() {
    this.models = [];
  }

  get storeName() { return this.constructor.name; }
  get eventSuffix() { return inflect(this.storeName, 'underscore', 'dasherize'); }

  uri(id) {
    return id ? `${this.baseUri}/${id}` : this.baseUri;
  }

  reset() {
    this.models = [];
    dispatch(`change${this.storeName}`, this.models);
  }

  sort(field, order) {
    this.models = this.models.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
    dispatch(`change${this.storeName}`, this.models);
  }

  getState(id) {
    if (!id) return this.models;
    return this.findModel(id);
  }

  findModel(id) {
    return this.models.filter((item) => {
      return item.id === id;
    })[0];
  }

  listenToEvent(event, ctx) {
    listenTo(`${event}-${this.eventSuffix}`, ctx);
  }

  stopListeningToEvent(event, ctx) {
    stopListeningTo(`${event}-${this.eventSuffix}`, ctx);
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
    dispatch(`change${this.storeName}`, this.models);
  }

  update(model) {
    if (Array.isArray(model)) {
      model.forEach((item) => {
        this.upsert(item);
      });
    } else {
      this.upsert(model);
    }
    dispatch(`change${this.storeName}`, model);
  }

  // @todo Error handling - Tivs
  fetchError(err) {
    console.error(err);
  }

  fetch(id) {
    Api.get(this.uri(id))
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }

  patch(id, changes) {
    Api.patch(this.uri(id), changes)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }

  post(id, data) {
    Api.post(this.uri(id), data)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }

  post(data) {
    Api.post(this.uri(), data)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }

  create(data) {
    Api.post(this.uri(), data)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }
}
