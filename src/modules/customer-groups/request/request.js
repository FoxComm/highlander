/* @flow */

import Clause from './clause';
import Condition from './condition';

export type RequestType = {
  query?: Object;
};

export default class Request extends Clause {

  _query: Condition;

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

  constructor(criterions: Array<any>) {
    super();
    this._criterions = criterions;
  }

  toRequest(): RequestType {
    const request = {};

    if (this.query.length) {
      request.query = {
        bool: this.query.toRequest()
      };
    }

    return request;
  }

}
