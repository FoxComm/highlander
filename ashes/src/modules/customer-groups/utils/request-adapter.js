/* @flow weak */

import get from 'lodash/get';
import { Request, query } from 'elastic/request';
import operators from 'paragons/customer-groups/operators';
import _ from 'lodash';

const requestAdapter = (groupId, criterions, mainCondition, conditions) => {
  const request = new Request(criterions);

  request.query = mainCondition === operators.and ? new query.ConditionAnd() : new query.ConditionOr();

  _.each(conditions, ([name, operator, value]) => {
    const field = (new query.Field(name)).add(operator, value);

    request.query.add(field);
  });

  return request;
};

export function fromRawQuery(query) {
  return (new Request()).raw(get(query, 'query'));
}

export default requestAdapter;
