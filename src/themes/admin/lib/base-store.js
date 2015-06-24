'use strict';

import Api from './api';
import { dispatch } from './dispatcher';

export default class BaseStore {
  constructor() {
    this.models = [];
  }

  get storeName() { return this.constructor.name; }

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

  add(model) {
    this.models.push(model);
    dispatch(`change${this.storeName}`, this.models);
  }

  update(model) {
    if (Array.isArray(model)) {
      Array.prototype.push.apply(this.models, model);
    } else {
      this.models.push(model);
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
}
