/* @flow */

import BucketAggregation from './bucket';


export default class HistogramAggregation extends BucketAggregation {

  _interval: number;

  constructor(name: string, field: string, interval: number) {
    super(name, field);
    this._interval = interval;
  }

  toRequest(): Object {
    return this.wrap({
      histogram: {
        field: this.field,
        interval: this._interval,
      },
    });
  }

}
