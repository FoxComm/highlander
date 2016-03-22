import _ from 'lodash';
import { booleanOperators, negateOperators, operatorsMap } from '../../../paragons/customer-groups/operators';


export default class Field {

  constructor(criterions, name) {
    const fieldCriterion = _.find(criterions, ({field}) => field === name);

    if (!fieldCriterion) {
      throw new TypeError(`No criterion found for field "${name}"`);
    }

    this._name = name;
    this._criterion = fieldCriterion;
    this._clauses = {};
  }

  toRequest() {
    const {field} = this._criterion;
    const and = [];
    const not = [];

    for (const operator in this._clauses) {
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


  set(clauses) {
    this._clauses = {};

    _.each(clauses, (value, operator) => this.add(operator, value));

    return this;
  }

  add(operator, value) {
    validateOperatorAppliance(this._criterion, operator);

    this._clauses[operator] = value;

    return this;
  }

}

const getQuery = (field, operator, value) => {
  const query = operatorsMap[operator](field, value);

  if (isDirect(field)) {
    return query;
  }

  return {
    nested: {
      path: field.slice(0, field.lastIndexOf('.')),
      query: query,
    },
  };
};

const isDirect = (field) => field.indexOf('.') === -1;

const validateOperatorAppliance = ({type, label, operators}, operator) => {
  if (!(operator in type.operators)) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${label}" of type "${type.name}"`);
  }

  if (operators && !(operators.includes(operator))) {
    throw new TypeError(`Operator ${operator} is not applicable for field "${label}"`);
  }
};
