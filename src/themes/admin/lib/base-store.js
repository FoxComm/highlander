'use strict';

import Api from './api';
import { dispatch, listenTo, stopListeningTo } from './dispatcher';
import { inflect } from 'fleck';

export default class BaseStore {
  constructor() {
    this.items = [];
  }

  get storeName() {
    return this.constructor.name;
  }

  get eventSuffix() {
    return inflect(this.storeName, 'underscore', 'dasherize');
  }

  dispatchChange() {
    dispatch(`change${this.storeName}`, this.items);
  }

  uri(id) {
    return id ? `${this.baseUri}/${id}` : this.baseUri;
  }

  reset() {
    this.items = [];
    this.dispatchChange();
  }

  sort(field, order) {
    this.items = this.items.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
    this.dispatchChange();
  }

  getState(id) {
    if (!id) return this.items;
    return this.getItem(id);
  }

  getItem(id) {
    return this.items.filter((item) => {
      return item.id === id;
    })[0];
  }

  listenToEvent(event, ctx) {
    listenTo(`${event}-${this.eventSuffix}`, ctx);
  }

  stopListeningToEvent(event, ctx) {
    stopListeningTo(`${event}-${this.eventSuffix}`, ctx);
  }

  process(item) {
    return item;
  }

  upsert(item) {
    let exists = this.getItem(item.id);
    if (exists) {
      let idx = this.items.indexOf(exists);
      if (idx !== -1) {
        this.items[idx] = this.process(item) || item;
      }
    } else {
      this.items.push(this.process(item) || item);
    }
  }

  add(item) {
    this.items.push(item);
    dispatch(`change${this.storeName}`, this.items);
  }

  update(item) {
    if (Array.isArray(item)) {
      item.forEach((item) => {
        this.upsert(item);
      });
    } else {
      this.upsert(item);
    }
    dispatch(`change${this.storeName}`, item);
  }

  // @todo Error handling - Tivs
  fetchError(err) {
    console.error(err);
  }

  fetch(id) {
    Api.get(this.uri(id))
      .then((res) => {
        this.update(res);
      })
      .catch((err) => {
        this.fetchError(err);
      });
  }

  patch(id, changes) {
    Api.patch(this.uri(id), changes)
      .then((res) => {
        this.update(res);
      })
      .catch((err) => {
        this.fetchError(err);
      });
  }

  create(data) {
    Api.post(this.uri(), data)
      .then((res) => {
        this.update(res);
      })
      .catch((err) => {
        this.fetchError(err);
      });
  }

  delete(id) {
    Api.delete(this.uri(id))
      .then((res) => {
        this.update(res);
      })
      .catch((err) => {
        this.fetchError(err);
      });
  }
}
