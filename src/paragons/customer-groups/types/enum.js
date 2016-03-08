import { validateOperatorAppliance } from './helpers';
import ops, { operatorsMap } from '../operators';


const type = {
  name: 'Enumeration',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.oneOf]: 'is one of',
    [ops.notOneOf]: 'is not one of',
  },
  getQuery: (field, operator, value) => {
    validateOperatorAppliance(operator, type, field);

    return operatorsMap[operator](field.field, value);
  },
};

export default type;
