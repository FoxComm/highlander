'use strict';

import BaseStore from './base-store.js';

export default class TableStore extends BaseStore {
  constructor(props) {
    super(props);
    this.columns = [];
    this.start = 0;
    this.limit = 2;
  }

  set uriRoot(uri) {
    this.rootUri = uri;
  }

  get rows() {
    return this.items.slice(this.start, this.start + this.limit);
  }

  setStart(value) {
    this.start = Math.max(0, Math.min(this.rows.length - this.limit, value));
    this.dispatchChange();
  }

  setLimit(value) {
    this.limit = Math.max(0, Math.min(this.rows.length, value));
    this.start = Math.min(this.start, this.rows.length - limit);
    this.dispatchChange();
  }

  setSorting(field, order) {
    this.items = this.items.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
    this.dispatchChange();
  }
}
