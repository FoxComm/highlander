/* @flow */

import MetricAggregation from './metric';


export default class CountAggregation extends MetricAggregation {

  constructor(name: string, field: string) {
    super(name, field);
  }

  toRequest(): Object {
    return this.wrap({
      value_count: {
        field: this.field,
      },
    });
  }

}
