/* @flow */

import Element from '../element';


export default class Aggregation extends Element {

  _name: string;

  get name() {
    return this._name;
  }

  constructor(name: string) {
    super();
    this._name = name;
  }

  toRequest: () => Object;

}
