/* @flow */

import Element from './element';
import Condition from './query/condition';
import Selector from './selector';
import Sorter from './sorter';
import Aggregator from './aggregations/aggregator';

export type RequestType = {
  _source?: Array<string>,
  query?: Object,
  sort?: Array<Object>,
};

export default class Request extends Element {

  _selector: Selector;

  _query: Condition;

  _sorter: Sorter;

  _aggregations: Aggregator;

  _criterions: Array<any>;

  _raw: Object;

  get criterions(): Array<any> {
    return this._criterions;
  }

  get select(): Selector {
    return this._selector || (this._selector = new Selector(this._criterions));
  }

  get query(): Condition {
    return this._query;
  }

  set query(value: Condition) {
    value.root = this;
    this._query = value;
  }

  get sort(): Sorter {
    return this._sorter || (this._sorter = new Sorter(this._criterions));
  }

  get aggregations(): Aggregator {
    if (this._aggregations) {
      return this._aggregations;
    }

    this._aggregations = new Aggregator();
    this._aggregations.root = this;

    return this._aggregations;
  }

  constructor(criterions: Array<any>) {
    super();
    this._criterions = criterions;
  }

  raw(query: Object) {
    this._raw = query;

    return this;
  }

  toRequest(): RequestType {
    const request = {};

    if (this.select.length) {
      request._source = this.select.toRequest();
    }

    if (this._raw) {
      request.query = this._raw;
    }

    if (this.query && this.query.length) {
      request.query = {
        bool: this.query.toRequest()
      };
    }

    if (this.sort.length) {
      request.sort = this.sort.toRequest();
    }

    if (this.aggregations.length) {
      request.aggs = this.aggregations.toRequest();
    }

    return request;
  }

}
