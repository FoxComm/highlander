import { validateOperatorAppliance } from './helpers';
import ops, { operatorsMap } from '../operators';


const type = {
  name: 'String',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.oneOf]: 'is one of',
    [ops.notOneOf]: 'is not one of',
    [ops.contains]: 'contains',
    [ops.notContains]: 'does not contain',
  },
  getQuery: (criterion, operator, value) => {
    validateOperatorAppliance(operator, type, criterion);

    return operatorsMap[operator](criterion.field, value);
  },
};

export default type;
