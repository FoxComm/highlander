import { validateOperatorAppliance } from './helpers';
import ops, { operatorsMap } from '../operators';


const type = {
  name: 'Number',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.less]: 'is before',
    [ops.greater]: 'is after',
    [ops.between]: 'is between',
  },
  getQuery: (criterion, operator, value) => {
    validateOperatorAppliance(operator, type, criterion);

    return operatorsMap[operator](criterion.field, value);
  },
};

export default type;
