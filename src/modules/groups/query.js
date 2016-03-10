import _ from 'lodash';
import operators, {
  booleanOperators,
  negateOperators,
  getQuery
} from '../../paragons/customer-groups/operators';

const buildQuery = (schema, query) => {
  return {
    filter: {
      bool: buildQueryBranch(schema, query),
    },
  };
};

const buildQueryBranch = (schema, query) => {
  const branch = {};

  //build or criterion
  if (operators.or in query && _.size(query[operators.or])) {
    const clauses = branch[booleanOperators.or] = [];
    for (const clause of query[operators.or]) {
      clauses.push({
        bool: buildQueryBranch(schema, clause),
      });
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
    const criterion = getCriterionSchema(schema, fieldName);

    const clauses = fields[fieldName];
    for (const operator in clauses) {
      const fieldQuery = getQuery(criterion, operator, clauses[operator]);

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

const getCriterionSchema = (schema, fieldName) => {
  const criterionSchema = _.find(schema, ({field}) => field === fieldName);

  if (!criterionSchema) {
    throw new TypeError(`No schema found for field "${fieldName}"`);
  }

  return criterionSchema;
};

export default buildQuery;
