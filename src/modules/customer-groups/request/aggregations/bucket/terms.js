/* @flow */

import BucketAggregation from './bucket';


export default class TermsAggregation extends BucketAggregation {

  _size: number = 10;

  constructor(name: string, field: string, size: number) {
    super(name, field);
    this._size = size;
  }

  toRequest(): Object {
    return this.wrap({
      terms: {
        field: this.field,
        size: this._size,
      }
    });
  }

}
