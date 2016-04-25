/* @flow */

import BucketAggregation from './bucket';

type Range = {
  from?: number|string;
  to?: number|string;
};

export default class HistogramAggregation extends BucketAggregation {

  _field: string;

  _ranges: Array<Range>;

  constructor(name: string, field: string, ranges: Array<Range>) {
    super(name);
    this._field = field;
    this._ranges = ranges;
  }

  toRequest(): Object {
    return {
      bucket: 'here'
    };
  }

}
