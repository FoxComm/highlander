/* @flow */

import Element from '../element';


export default class Condition extends Element {

  _conditions: Array<Condition>;

  toRequest(): Object {
    return {};
  };

  get length(): number {
    return this._conditions.length;
  }

  constructor() {
    super();
    this._conditions = [];
  }

  add(condition: Condition): Condition {
    condition.root = this.root;

    this._conditions.push(condition);

    return this;
  }

  set(conditions: Array<Condition>): Condition {
    conditions.forEach(condition => {
      condition.root = this.root;
    });

    this._conditions = conditions;

    return this;
  }

  reset(): Condition {
    this._conditions.forEach(condition => {
      condition.root = null;
    });

    this._conditions = [];

    return this;
  }

}
