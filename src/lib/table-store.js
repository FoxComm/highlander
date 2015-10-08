'use strict';

import BaseStore from './base-store.js';

export default class TableStore extends BaseStore {
  static SORTING_ORDER = {
    ASC: true,
    DESC: false
  };

  constructor(props) {
    super(props);
    this.columns = [];
    this.start = 0;
    this.limit = 25;
    this.sortingField = null;
    this.sortingOrder = false;
    this.addListener('change', this._onChange.bind(this));
  }

  _onChange() {
    this._sort();
  }

  _sort() {
    let field = this.sortingField;
    let order = this.sortingOrder;
    this.models = this.models.sort((a, b) => {
      return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
    });
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
    if (this.sortingField !== field || this.sortingOrder !== order) {
      this.sortingField = field;
      this.sortingOrder = !!order;
      this.notifyChanged();
    }
  }
}
