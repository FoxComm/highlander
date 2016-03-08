const operators = {
  equal: 'equal',
  notEqual: 'notEqual',
  oneOf: 'oneOf',
  notOneOf: 'notOneOf',
  contains: 'contains',
  notContains: 'notContains',
  greater: 'greater',
  less: 'less',
  between: 'between',
  and: '$and',
  not: '$not',
  or: '$or',
};

export const negateOperators = {
  notEqual: operators.equal,
  notOneOf: operators.oneOf,
  notContains: operators.contains,
};

export const booleanOperators = {
  and: 'must',
  not: 'must_not',
  or: 'should',
};
export default operators;


export const operatorsMap = {
  equal: (fieldName, data) => ({
    term: {
      [fieldName]: data,
    },
  }),
  oneOf: (fieldName, data) => ({
    terms: {
      [fieldName]: data,
    },
  }),
  contains: (fieldName, data) => ({
    match: {
      [fieldName]: data,
    },
  }),
  greater: (fieldName, data) => ({
    range: {
      [fieldName]: {
        gte: data,
      },
    },
  }),
  less: (fieldName, data) => ({
    range: {
      [fieldName]: {
        lte: data,
      },
    },
  }),
  between: (fieldName, [min, max]) => ({
    range: {
      [fieldName]: {
        gte: min,
        lte: max,
      },
    },
  }),
};

//negate operators return the same as their positive analogs
operatorsMap.notEqual = operatorsMap.equal;
operatorsMap.notOneOf = operatorsMap.oneOf;
operatorsMap.notContains = operatorsMap.contains;
