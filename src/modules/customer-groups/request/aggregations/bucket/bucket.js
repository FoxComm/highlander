/* @flow */

import _ from 'lodash';
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

  wrap(field: string, aggregation: Object): Object {
    const aggregations = {};

    this._aggregations.forEach(aggregation => {
      aggregations[aggregation.name] = aggregation.toRequest();
    });

    if (isDirectField(field)) {
      if (_.isEmpty(aggregations)) {
        return aggregation;
      }

      return {
        ...aggregation,
        aggregations,
      };
    }

    if (_.isEmpty(aggregations)) {
      return {
        nested: {
          path: getNestedPath(field)
        },
        aggregations: {
          [this.name]: aggregation,
        }
      };
    }

    return {
      nested: {
        path: getNestedPath(field)
      },
      aggregations: {
        [this.name]: {
          ...aggregation,
          aggregations: {
            reverseNested: {
              reverse_nested: {},
              aggregations
            },
          },
        },
      },
    };
  }

  combine(aggregation, aggregations) {
    if (_.isEmpty(aggregations)) {
      return aggregation;
    }

    return {
      ...aggregation,
      aggregations,
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
