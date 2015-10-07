'use strict';

import BaseStore from './base-store.js';

export default class TableStore extends BaseStore {
  constructor(props) {
    super(props);
    this.columns = [];
    this.start = 0;
    this.limit = 25;
  }

  set uriRoot(uri) {
    this.rootUri = uri;
  }

  get rows() {
    return this.models.slice(this.start, this.start + this.limit);
  }

  setStart(value) {
    this.start = Math.max(0, Math.min(this.models.length - this.limit, value));
    this.notifyChanged();
  }

  setLimit(value) {
    this.limit = Math.max(0, Math.min(this.models.length, value));
    this.start = Math.min(this.start, this.models.length - this.limit);
    this.notifyChanged();
  }

  setSorting(field, order) {
    this.models = this.models.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
    this.notifyChanged();
  }
}
