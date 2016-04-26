/* @flow */

import Element from './element';
import Condition from './query/condition';
import Sorter from './sorter';
import Aggregator from './aggregations/aggregator';


export type RequestType = {
  query?: Object;
  sort?: Array<Object>;
};


export default class Request extends Element {

  _query: Condition;

  _sorter: Sorter;

  _aggregations: Aggregator;

  _criterions: Array<any>;

  get criterions(): Array<any> {
    return this._criterions;
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

  toRequest(): RequestType {
    const request = {};

    if (this.query && this.query.length) {
      request.query = {
        bool: this.query.toRequest()
      };
    }

    if (this.sort.length) {
      request.sort = this.sort.toRequest();
    }

    if (this.aggregations.length) {
      request.aggregations = this.aggregations.toRequest();
    }

    return request;
  }

}
