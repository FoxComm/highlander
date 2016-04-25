/* @flow */

import BucketAggregation from './bucket';


export default class TermsAggregation extends BucketAggregation {

  _field: string;

  _size: number;

  constructor(name: string, field: string, size: number) {
    super(name);
    this._field = field;
    this._size = size;
  }

  toRequest(): Object {
    return {
      bucket: 'here'
    };
  }

}
