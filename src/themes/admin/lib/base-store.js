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

  upsert(model) {
    let exists = this.findModel(model.id);
    if (exists) {
      let idx = this.models.indexOf(exists);
      if (idx !== -1) {
        this.models[idx] = model;
      }
    } else {
      this.models.push(model);
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
    console.log(err);
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

  create(data) {
    Api.post(this.uri(), data)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }
}
