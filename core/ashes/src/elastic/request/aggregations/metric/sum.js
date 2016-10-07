/* @flow */

import MetricAggregation from './metric';


export default class SumAggregation extends MetricAggregation {

  constructor(name: string, field: string) {
    super(name, field);
  }

  toRequest(): Object {
    return this.wrap({
      sum: {
        field: this.field,
      },
    });
  }

}
