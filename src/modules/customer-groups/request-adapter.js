//libs
import _ from 'lodash';

//data
import { Request, ConditionAnd, ConditionOr, Field } from './request';
import operators from '../../paragons/customer-groups/operators';


const requestAdapter = (criterions, mainCondition, conditions) => {
  const request = new Request(criterions);
  const query = request.query = mainCondition === operators.and ? new ConditionAnd() : new ConditionOr();

  let fields = {};
  for (let i = 0; i < conditions.length; i++) {
    const [name, operator, value] = conditions[i];

    let field;
    if (fields[name]) {
      field = fields[name];
    } else {
      field = fields[name] = new Field(name);
      query.add(field);
    }

    field.add(operator, value);
  }

  return request;
};

export default requestAdapter;
