'use strict';

import Api from './api';
import { dispatch } from './dispatcher';

export default class BaseStore {
  constructor() {
    this.models = [];
  }

  getState(id) {
    if (!id) return this.models;
    let found = this.models.filter((item) => {
      return item.id === id;
    });
    return found[0];
  }

  update(model) {
    if (Array.isArray(model)) {
      Array.prototype.push.apply(this.models, model);
    } else {
      this.models.push(model);
    }
    dispatch('change', model);
  }

  // @todo Error handling - Tivs
  fetchError(err) {
    console.log(err);
  }

  fetch(id) {
    let uri = id ? `${this.baseUri}/${id}` : this.baseUri;
    Api.get(uri)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }
}
