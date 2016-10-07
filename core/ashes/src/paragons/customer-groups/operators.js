const operators = {
  equal: 'equal',
  notEqual: 'notEqual',
  oneOf: 'oneOf',
  notOneOf: 'notOneOf',
  contains: 'contains',
  containsNotAnalyzed: 'containsNotAnalyzed',
  notContains: 'notContains',
  greater: 'greater',
  less: 'less',
  between: 'between',
  and: '$and',
  not: '$not',
  or: '$or',
};

export default operators;

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
  containsNotAnalyzed: (fieldName, data) => ({
    match: {
      [fieldName]: {query: data, analyzer: 'standard'},
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

export const getQuery = (criterion, operator, value) => {
  const query = criterion.type.getQuery(criterion, operator, value);
  const fieldName = criterion.field;

  if (isDirect(fieldName)) {
    return query;
  }

  return {
    nested: {
      path: fieldName.slice(0, fieldName.lastIndexOf('.')),
      query: query,
    },
  };
};

const isDirect = (field) => field.indexOf('.') === -1;

//negate operators return the same as their positive analogs
operatorsMap.notEqual = operatorsMap.equal;
operatorsMap.notOneOf = operatorsMap.oneOf;
operatorsMap.notContains = operatorsMap.contains;
