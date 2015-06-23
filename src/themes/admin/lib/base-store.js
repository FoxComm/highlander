'use strict';

import Api from './api';
import { dispatch } from './dispatcher';

export default class BaseStore {
  constructor() {
    this.models = [];
  }

  uri(id) {
    return id ? `${this.baseUri}/${id}` : this.baseUri;
  }

  getState(id) {
    if (!id) return this.models;
    let found = this.models.filter((item) => {
      return item.id === id;
    });
    return found[0];
  }

  update(model) {
    let storeName = this.constructor.name;
    if (Array.isArray(model)) {
      Array.prototype.push.apply(this.models, model);
    } else {
      this.models.push(model);
    }
    dispatch(`change${storeName}`, model);
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
