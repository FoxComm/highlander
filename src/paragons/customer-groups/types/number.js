import { validateOperatorAppliance } from './helpers';
import ops, { operatorsMap } from '../operators';


const type = {
  name: 'Number',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.oneOf]: 'is one of',
    [ops.notOneOf]: 'is not one of',
    [ops.greater]: 'is greater than',
    [ops.less]: 'is less than',
    [ops.between]: 'is between',
  },
  getQuery: (criterion, operator, value) => {
    validateOperatorAppliance(operator, type, criterion);

    return operatorsMap[operator](criterion.field, value);
  },
};

export default type;
