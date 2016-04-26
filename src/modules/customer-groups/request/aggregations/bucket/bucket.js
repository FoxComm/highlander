/* @flow */

import _ from 'lodash';
import { isDirectField, getNestedPath } from '../../helpers';
import Aggregation from '../aggregation';


export default class BucketAggregation extends Aggregation {

  _aggregations: Array<Aggregation>;

  constructor(name: string, field?: string) {
    super(name, field);
    this._aggregations = [];
  }

  wrap(aggregation: Object): Object {
    console.log(`${this.name}(${this.field}) in ${this.inheritedPath}`);
    const aggregations = {};

    this._aggregations.forEach(aggregation => {
      aggregations[aggregation.name] = aggregation.toRequest();
    });

    if (isDirectField(this.field)) {
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
          path: getNestedPath(this.field)
        },
        aggregations: {
          [this.name]: aggregation,
        }
      };
    }

    return {
      nested: {
        path: getNestedPath(this.field)
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

  add(aggregation: Aggregation): BucketAggregation {
    aggregation.root = this.root;
    aggregation.inheritedPath = !this.field || isDirectField(this.field) ? null : getNestedPath(this.field);

    this._aggregations.push(aggregation);

    return this;
  }

  set(aggregations: Array<Aggregation>): BucketAggregation {
    this.reset();

    aggregations.forEach(aggregation => this.add(aggregation));

    return this;
  }

  reset() {
    this._aggregations.forEach(aggregation => {
      aggregation.root = null;
      aggregation.inheritedPath = null;
    });

    this._aggregations = [];

    return this;
  }

}
