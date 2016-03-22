//libs
import _ from 'lodash';

//data
import { Query, ConditionAnd, ConditionOr, Field } from './query';
import operators from '../../paragons/customer-groups/operators';


const queryAdapter = (criterions, mainCondition, conditions) => {
  const query = new Query(criterions);
  const main = query.main = mainCondition === operators.and ? query.and() : query.or();

  let fields = {};
  for (let i = 0; i < conditions.length; i++) {
    const [name, operator, value] = conditions[i];

    let field;
    if (fields[name]) {
      field = fields[name];
    } else {
      field = fields[name] = query.field(name);
      main.add(field);
    }

    field.add(operator, value);
  }

  return query;
};

export default queryAdapter;
