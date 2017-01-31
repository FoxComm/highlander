/* @flow */

import _ from 'lodash';
import { isDirectField, getNestedPath } from '../../helpers';
import {
  wrapInheritedDirectLeave,
  wrapInheritedDirectParent,
  wrapPlainDirectLeave,
  wrapPlainDirectParent,
  wrapPlainIndirectLeave,
  wrapPlainIndirectParent,
} from '../wrappings';
import Aggregation from '../aggregation';


export default class BucketAggregation extends Aggregation {

  _aggregations: Array<Aggregation>;

  constructor(name: string, field?: string) {
    super(name, field);
    this._aggregations = [];
  }

  wrap(aggregation: Object): Object {
    const aggregations = this._aggregations.reduce((res, aggregation) => {
      res[aggregation.name] = aggregation.toRequest();

      return res;
    }, {});

    //inheritance || direct field || aggregations given
    if (!this.inheritedPath) {
      if (isDirectField(this.field)) {
        return _.isEmpty(aggregations)
          ? wrapPlainDirectLeave.call(this, aggregation)
          : wrapPlainDirectParent.call(this, aggregation, aggregations);
      }

      return _.isEmpty(aggregations)
        ? wrapPlainIndirectLeave.call(this, aggregation)
        : wrapPlainIndirectParent.call(this, aggregation, aggregations);
    }

    if (isDirectField(this.field)) {
      return _.isEmpty(aggregations)
        ? wrapInheritedDirectLeave.call(this, aggregation)
        : wrapInheritedDirectParent.call(this, aggregation, aggregations);
    }

    if (getNestedPath(this.field) === this.inheritedPath) {
      return _.isEmpty(aggregations)
        ? wrapPlainDirectLeave.call(this, aggregation)
        : wrapPlainDirectParent.call(this, aggregation, aggregations);
    }

    return _.isEmpty(aggregations)
      ? wrapPlainIndirectLeave(this, wrapInheritedDirectLeave.call(this, aggregation))
      : wrapPlainIndirectParent(this, wrapInheritedDirectLeave.call(this, aggregation), aggregations);
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

  reset(): BucketAggregation {
    this._aggregations.forEach(aggregation => {
      aggregation.root = null;
      aggregation.inheritedPath = null;
    });

    this._aggregations = [];

    return this;
  }

}
