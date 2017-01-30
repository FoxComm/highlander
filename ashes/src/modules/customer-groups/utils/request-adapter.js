/* @flow weak */

import isEmpty from 'lodash/isEmpty';
import { Request, query } from 'elastic/request';
import operators from 'paragons/customer-groups/operators';
import _ from 'lodash';

const requestAdapter = (groupId, criterions, mainCondition, conditions) => {
  const request = new Request(criterions);

  // if have no conditions from clientState request (UI builder) just use groupId to fetch customers
  if (isEmpty(conditions)) {
     request.query = (new query.Field('groups')).add(operators.equal, groupId);

     return request;
  }

  request.query = mainCondition === operators.and ? new query.ConditionAnd() : new query.ConditionOr();

  let fields = {};
  _.each(conditions, (condition) => {
    const [name, operator, value] = condition;

    let field;
    if (fields[name]) {
      field = fields[name];
    } else {
      field = fields[name] = new query.Field(name);
      request.query.add(field);
    }

    field.add(operator, value);
  });

  return request;
};

export default requestAdapter;
