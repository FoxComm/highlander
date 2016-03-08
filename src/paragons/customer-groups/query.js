import _ from 'lodash';
import operators, { booleanOperators, negateOperators } from './operators';

//internal structure:
const query = {
  name: {
    contains: 'Anika'
  },
  revenue: {
    greater: 5,
    less: 10
  },
  $or: {}
};


const buildQuery = (schema, query) => {
  return {
    filter: {
      bool: buildQueryBranch(schema, query),
    }
  }
};

const buildQueryBranch = (schema, query) => {
  const branch = {};

  //build or criterion
  if (operators.or in query) {
    const innerBranch = buildQueryBranch(schema, query[operators.or]);
    if (_.size(innerBranch)) {
      branch[booleanOperators.or] = {
        bool: innerBranch,
      };
    }
  }

  //add query branch fields, if any
  const fields = _.omit(query, operators.or);
  if (_.size(fields)) {
    return {
      ...branch,
      ...getQueryBranchFields(schema, fields),
    };
  }

  return branch;
};

const getQueryBranchFields = (schema, fields) => {
  const positiveClauses = [];
  const negativeClauses = [];

  for (const fieldName in fields) {
    const field = getFieldSchema(schema, fieldName);

    const clauses = fields[fieldName];
    for (const operator in clauses) {
      const fieldQuery = field.type.getQuery(field, operator, clauses[operator]);

      if (operator in negateOperators) {
        negativeClauses.push(fieldQuery);
      } else {
        positiveClauses.push(fieldQuery);
      }
    }
  }

  const result = {};
  if (positiveClauses.length) {
    result[booleanOperators.and] = positiveClauses;
  }
  if (negativeClauses.length) {
    result[booleanOperators.not] = negativeClauses;
  }

  return result;
};

const getFieldSchema = (schema, fieldName) => {
  const fieldSchema = _.find(schema, ({field}) => field === fieldName);

  if (!fieldSchema) {
    throw new TypeError(`No schema found for field "${fieldName}"`);
  }

  return fieldSchema;
};

global.query = query;
global.buildQuery = buildQuery;

export default buildQuery;
