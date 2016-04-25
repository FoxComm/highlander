/* @flow */

import { isDirectField, getNestedPath } from '../../helpers';
import Aggregation from '../aggregation';


export default class BucketAggregation extends Aggregation {

  _aggregations: Array<Aggregation>;

  constructor(name: string) {
    super(name);
    this._aggregations = [];
  }

  toRequest(): Object {
    return {
      bucket: 'here'
    };
  }

  add(condition: Aggregation): BucketAggregation {
    condition.root = this.root;

    this._aggregations.push(condition);

    return this;
  }

  set(conditions: Array<Aggregation>): BucketAggregation {
    conditions.forEach(condition => {
      condition.root = this.root;
    });

    this._aggregations = conditions;

    return this;
  }

  reset() {
    this._aggregations.forEach(condition => {
      condition.root = null;
    });

    this._aggregations = [];

    return this;
  }

}
