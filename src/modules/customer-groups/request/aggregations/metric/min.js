/* @flow */

import MetricAggregation from './metric';


export default class MinAggregation extends MetricAggregation {

  constructor(name: string, field: string) {
    super(name, field);
  }

  toRequest(): Object {
    return this.wrap({
      min: {
        field: this.field,
      },
    });
  }

}
