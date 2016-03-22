import { booleanOperators } from '../../../paragons/customer-groups/operators';
import ConditionOr from './condition-or';
import Condition from './condition';


export default class ConditionAnd extends Condition {

  constructor(criterions) {
    super(criterions);
  }

  toRequest() {
    const result = [];
    for (const condition of this._conditions) {
      result.push({bool: condition.toRequest()});
    }

    return {
      [booleanOperators.and]: result,
    };
  }

  aaatoRequest() {
    const and = [];
    const or = [];
    const not = [];

    for (const condition of this._conditions) {
      const request = condition.toRequest();

      //if and
      if (condition instanceof ConditionAnd) {
        and.push(request);
        //if or
      } else if (condition instanceof ConditionOr) {
        or.push(request);
        //if field
      } else {
        if (request[booleanOperators.and]) {
          and.push(request[booleanOperators.and]);
        }
        if (request[booleanOperators.not]) {
          not.push(request[booleanOperators.not]);
        }
      }
    }

    const result = {};
    if (and.length) {
      result[booleanOperators.and] = and;
    }
    if (or.length) {
      result[booleanOperators.or] = or;
    }
    if (not.length) {
      result[booleanOperators.not] = not;
    }

    return {
      bool: {
        ...result,
      },
    };
  }

}
