/* @flow */

import MetricAggregation from './metric';


export default class AverageAggregation extends MetricAggregation {

  _field: string;

  constructor(name: string, field: string) {
    super(name);
    this._field = field;
  }

  toRequest(): Object {
    return this.wrap(this._field, {
      avg: {
        field: this._field,
      },
    });
  }

}
