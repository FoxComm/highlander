/* @flow */

import Element from '../element';


export default class Aggregation extends Element {

  name: string;

  field: ?string = null;

  inheritedPath: ?string;

  constructor(name: string, field: ?string) {
    super();
    this.name = name;
    this.field = field;
  }

  toRequest(): Object {
    return {};
  };

}
