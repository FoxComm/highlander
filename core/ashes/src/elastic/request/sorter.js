/* @flow */

import { isDirectField, getNestedPath } from './helpers';


type SortingField = {
  name: string;
  order: ?string;
};

export default class Sorter {

  _criterions: Array<any>;

  _fields: Array<SortingField> = [];

  get length(): number {
    return this._fields.length;
  }

  constructor(criterions: Array<any>) {
    this._criterions = criterions;
  }

  toRequest(): Array<Object> {
    const result = [];

    for (const {name, order} of this._fields) {
      const field = {
        [name]: {
          order: order === 'asc' ? 'asc' : 'desc',
        },
      };

      if (!isDirectField(name)) {
        field[name].nested_path = getNestedPath(name);
      }

      result.push(field);
    }

    return result;
  }

  add(name: string, order: string): Sorter {
    this._fields.push({name, order});

    return this;
  }

  set(fields: Array<SortingField>): Sorter {
    this._fields = fields;

    return this;
  }

  reset(): Sorter {
    this._fields = [];

    return this;
  }

}
