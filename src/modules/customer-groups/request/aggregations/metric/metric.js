/* @flow */

import Aggregation from '../aggregation';
import { isDirectField, getNestedPath } from '../../helpers';


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

  wrap(field: string, aggregation: Object): Object {
    if (isDirectField(field)) {
      return aggregation;
    }

    return {
      nested: {
        path: getNestedPath(field)
      },
      aggregations: {
        [this.name]: aggregation,
      },
    };
  }

}
