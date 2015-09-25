'use strict';

import _ from 'lodash';
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

  findModel(baseId, id) {
    return this._findModel(this.models[baseId], id);
  }

  findWhere(baseId, ...args) {
    return this._findWhere(this.models[baseId], ...args);
  }

  notifyChanged(baseId) {
    this.dispatch('change', baseId, this.models[baseId]);
  }

  update(baseId, res) {
    if (!this.models[baseId]) {
      this.models[baseId] = _.isArray(res) ? res : [res];
    }
    this._update(this.models[baseId], res);
    this.notifyChanged(baseId);
  }

  updateBehaviour(res, baseId) {
    this.update(baseId, res);
  }

  patch(baseId, id, changes) {
    return super.patch(changes, baseId, id);
  }

  create(baseId, data) {
    return super.create(data, baseId);
  }
}
