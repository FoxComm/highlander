/* @flow */

import Aggregation from '../aggregation';


export default class MetricAggregation extends Aggregation {

  constructor(name: string) {
    super(name);
    this._aggregations = [];
  }

  toRequest(): Object {
    return {
      bucket: 'here'
    };
  }

}
