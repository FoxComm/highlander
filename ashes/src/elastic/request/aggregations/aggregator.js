/* @flow */

import Element from '../element';
import Aggregation from './aggregation';


export default class Aggregator extends Element {

  _aggregations: Array<Aggregation> = [];

  get length(): number {
    return this._aggregations.length;
  }

  constructor() {
    super();
  }

  toRequest(): Object {
    return this._aggregations.reduce((res, aggregation) => {
        res[aggregation.name] = aggregation.toRequest();

        return res;
      }, {});
  }

  add(aggregation: Aggregation): Aggregator {
    aggregation.root = this.root;
    aggregation.inheritedPath = null;

    this._aggregations.push(aggregation);

    return this;
  }

  set(aggregations: Array<Aggregation>): Aggregator {
    this.reset();

    aggregations.forEach(aggregation => this.add(aggregation));

    return this;
  }

  reset(): Aggregator {
    this._aggregations.forEach(aggregation => {
      aggregation.root = null;
      aggregation.inheritedPath = null;
    });

    this._aggregations = [];

    return this;
  }

}
