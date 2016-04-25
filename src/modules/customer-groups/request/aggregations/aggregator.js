/* @flow */

import Element from '../element';
import Aggregation from '../aggregations/aggregation';


export default class Aggregator extends Element {

  _criterions: Array<any>;

  _aggregations: Array<Aggregation> = [];

  get length(): number {
    return this._aggregations.length;
  }

  constructor(criterions: Array<any>) {
    super();
    this._criterions = criterions;
  }

  toRequest(): Object {
    return {
      bucket: 'here'
    };
  }

  add(condition: Aggregation): Aggregator {
    condition.root = this.root;

    this._aggregations.push(condition);

    return this;
  }

  set(conditions: Array<Aggregation>): Aggregator {
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
