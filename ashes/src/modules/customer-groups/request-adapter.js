import { Request, query } from '../../elastic/request';
import operators from '../../paragons/customer-groups/operators';

const requestAdapter = (criterions, mainCondition, conditions) => {
  const request = new Request(criterions);
  request.query = mainCondition === operators.and ? new query.ConditionAnd() : new query.ConditionOr();

  let fields = {};
  for (let i = 0; i < conditions.length; i++) {
    const [name, operator, value] = conditions[i];

    let field;
    if (fields[name]) {
      field = fields[name];
    } else {
      field = fields[name] = new query.Field(name);
      request.query.add(field);
    }

    field.add(operator, value);
  }

  return request;
};

export default requestAdapter;
