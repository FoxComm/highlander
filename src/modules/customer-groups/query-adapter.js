//libs
import _ from 'lodash';

//data
import operators from '../../paragons/customer-groups/operators';


const queryAdapter = (mainCondition, conditions) => {
  const clauses = buildFieldClauses(conditions);

  if (mainCondition === operators.and) {
    return clauses;
  }

  return buildOr(clauses);
};

const buildFieldClauses = conditions => {
  let clauses = {};

  for (let i = 0; i < conditions.length; i++) {
    const [field, operator, value] = conditions[i];
    let clause = clauses[field] || (clauses[field] = {});
    clause[operator] = value;
  }

  return clauses;
};

const buildOr = clauses => {
  let choices = [];

  for (const field in clauses) {
    choices.push({[field]: clauses[field]});
  }

  return {
    [operators.or]: choices,
  };
};

export default queryAdapter;
