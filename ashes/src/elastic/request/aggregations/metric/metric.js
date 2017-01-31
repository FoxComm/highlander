/* @flow */

import Aggregation from '../aggregation';
import { isDirectField, getNestedPath } from '../../helpers';
import {
  wrapInheritedDirectLeave,
  wrapPlainDirectLeave,
  wrapPlainIndirectLeave,
} from '../wrappings';

export default class MetricAggregation extends Aggregation {

  constructor(name: string, field?: string) {
    super(name, field);
  }

  wrap(aggregation: Object): Object {
    //inheritance || direct field || aggregations given
    if (!this.inheritedPath) {
      return isDirectField(this.field)
        ? wrapPlainDirectLeave.call(this, aggregation)
        : wrapPlainIndirectLeave.call(this, aggregation);
    }

    if (isDirectField(this.field)) {
      return wrapInheritedDirectLeave.call(this, aggregation);
    }

    if (getNestedPath(this.field) === this.inheritedPath) {
      return wrapPlainDirectLeave.call(this, aggregation);
    }

    return wrapPlainIndirectLeave(this, wrapInheritedDirectLeave.call(this, aggregation));
  }

}
