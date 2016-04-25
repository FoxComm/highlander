/* @flow */

import MetricAggregation from './metric';


export default class MinAggregation extends MetricAggregation {

  _field: string;

  constructor(name: string, field: string) {
    super(name);
    this._field = field;
  }

  toRequest(): Object {
    return {
      bucket: 'here'
    };
  }

}
