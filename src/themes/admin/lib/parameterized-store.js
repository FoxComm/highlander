'use strict';

import Api from './api';
import EventedStore from './evented-store';

export default class ParameterizedStore extends EventedStore {
  constructor() {
    super();
    this.models = {};
  }

  baseUri(baseId) {
    throw new Error('baseUri not implemented');
  }

  uri(baseId, id=null) {
    let uri = this.baseUri(baseId);

    return id !== null ? `${uri}/${id}` : uri;
  }

  notifyChanged(baseId) {
    this.dispatch('change', baseId, this.models[baseId]);
  }

  update(baseId, res) {
    this.models[baseId] = res;
    this.notifyChanged(baseId);
  }

  fetch(baseId, id=null) {
    return Api.get(this.uri(baseId, id))
      .then((res) => { this.update(baseId, res); return res; })
      .catch((err) => { this.apiError(err); return err; });
  }

  patch(baseId, id, changes) {
    return Api.patch(this.uri(baseId, id), changes)
      .then((res) => { this.update(res); return res; })
      .catch((err) => { this.apiError(err); return err; });
  }

  create(baseId, data) {
    return Api.post(this.uri(baseId), data)
      .then((res) => { this.update(res); return res; })
      .catch((err) => { this.apiError(err); return err; });
  }
}
