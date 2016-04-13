import { booleanOperators } from '../../../paragons/customer-groups/operators';
import Condition from './condition';


export default class ConditionOr extends Condition {

  constructor(criterions) {
    super(criterions);
  }

  toRequest() {
    if (this._conditions.length === 1) {
      return this._conditions[0].toRequest();
    }

    const result = [];
    for (const condition of this._conditions) {
      result.push({bool: condition.toRequest()});
    }

    return {
      [booleanOperators.or]: result,
    };
  }

}
