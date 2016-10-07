/* @flow */

export default class Selector {

  _criterions: Array<any>;

  _fields: Array<string> = [];

  get length(): number {
    return this._fields.length;
  }

  constructor(criterions: Array<any>) {
    this._criterions = criterions;
  }

  toRequest(): Array<string> {
    return this._fields.slice();
  }

  add(name: string): Selector {
    this._fields.push(name);

    return this;
  }

  set(fields: Array<string>): Selector {
    this.reset();

    fields.forEach(field => this.add(field));

    return this;
  }

  reset(): Selector {
    this._fields = [];

    return this;
  }

}
