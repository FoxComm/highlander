/* @flow */

import MetricAggregation from './metric';


export default class MaxAggregation extends MetricAggregation {

  constructor(name: string, field: string) {
    super(name, field);
  }

  toRequest(): Object {
    return this.wrap({
      avg: {
        field: this.field,
      },
    });
  }

}
