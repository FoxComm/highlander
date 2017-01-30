/* @flow */

import _ from 'lodash';
import Element from './../element';
import { isDirectField, getNestedPath } from '../helpers';
import {
  booleanOperators,
  negateOperators,
  operatorsMap,
} from '../../../paragons/customer-groups/operators';


export default class Field extends Element {

  _name: string;
  _clauses: Object;

  constructor(name: string) {
    super();
    this._name = name;
    this._clauses = {};
  }

  toRequest(): Object {
    if (!this.root) {
      throw new TypeError(`Not attached to request query`);
    }

    const criterion = _.find(this.root.criterions, ({field}) => field === this._name);

    if (!criterion) {
      throw new TypeError(`No criterion found for field "${this._name}"`);
    }

    const {field} = criterion;
    const and = [];
    const not = [];

    for (const operator in this._clauses) {
      validateOperatorAppliance(criterion, operator);

      const fieldQuery = getQuery(field, operator, this._clauses[operator]);

      (operator in negateOperators ? not : and).push(fieldQuery);
    }

    const result = {};
    if (and.length) {
      result[booleanOperators.and] = and;
    }
    if (not.length) {
      result[booleanOperators.not] = not;
    }

    return result;
  }

  get length(): number {
    return Object.keys(this._clauses).length;
  }


  set(clauses: Object): Field {
    this._clauses = clauses;

    return this;
  }

  reset(): Field {
    this._clauses = {};

    return this;
  }

  add(operator: string, value: Object): Field {
    this._clauses[operator] = value;

    return this;
  }

}

const getQuery = (field, operator, value) => {
  const query = operatorsMap[operator](field, value);

  if (isDirectField(field)) {
    return query;
  }

  return {
    nested: {
      path: getNestedPath(field),
      query: query,
    },
  };
};

const validateOperatorAppliance = ({type, label, operators}, operator) => {
  if (!(operator in type.operators)) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${label}" of type "${type.name}"`);
  }

  if (operators && !(operators.includes(operator))) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${label}"`);
  }
};
