/* @flow */

import Element from '../element';
import Aggregation from '../aggregations/aggregation';


export default class Aggregator extends Element {

  _aggregations: Array<Aggregation> = [];

  get length(): number {
    return this._aggregations.length;
  }

  constructor() {
    super();
  }

  toRequest(): Object {
    const result = {};

    this._aggregations.forEach(aggregation => {
      result[aggregation.name] = aggregation.toRequest();
    });

    return result;
  }

  add(aggregation: Aggregation): Aggregator {
    aggregation.root = this.root;

    this._aggregations.push(aggregation);

    return this;
  }

  set(aggregations: Array<Aggregation>): Aggregator {
    aggregations.forEach(aggregation => {
      aggregation.root = this.root;
    });

    this._aggregations = aggregations;

    return this;
  }

  reset() {
    this._aggregations.forEach(aggregation => {
      aggregation.root = null;
    });

    this._aggregations = [];

    return this;
  }

}
