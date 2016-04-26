/* @flow */

import Aggregation from '../aggregation';
import { isDirectField, getNestedPath } from '../../helpers';


export default class MetricAggregation extends Aggregation {

  constructor(name: string, field?: string) {
    super(name, field);
  }

  wrap(aggregation: Object): Object {
    console.log(`${this.name}(${this.field}) in ${this.inheritedPath}`);
    if (isDirectField(this.field)) {
      return aggregation;
    }

    return {
      nested: {
        path: getNestedPath(this.field)
      },
      aggregations: {
        [this.name]: aggregation,
      },
    };
  }

}
