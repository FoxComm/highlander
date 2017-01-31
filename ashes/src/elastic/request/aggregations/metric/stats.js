/* @flow */

import MetricAggregation from './metric';

export default class StatsAggregation extends MetricAggregation {

  constructor(name: string, field: string) {
    super(name, field);
  }

  toRequest(): Object {
    return this.wrap({
      stats: {
        field: this.field,
      },
    });
  }

}
