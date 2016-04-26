/* @flow */

import BucketAggregation from './bucket';


export default class HistogramAggregation extends BucketAggregation {

  _field: string;

  _interval: number;

  constructor(name: string, field: string, interval: number) {
    super(name);
    this._field = field;
    this._interval = interval;
  }

  toRequest(): Object {
    return this.wrap(this._field, {
      histogram: {
        field: this._field,
        interval: this._interval,
      },
    });
  }

}
