/* @flow */

import BucketAggregation from './bucket';

type Range = {
  from?: number|string,
  to?: number|string,
};

export default class RangeAggregation extends BucketAggregation {

  _ranges: Array<Range>;

  constructor(name: string, field: string, ranges: Array<Range>) {
    super(name, field);
    this._ranges = ranges;
  }

  toRequest(): Object {
    return this.wrap({
      date_range: {
        field: this.field,
        ranges: this._ranges,
      }
    });
  }

}
