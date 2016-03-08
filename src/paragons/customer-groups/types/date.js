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
  getQuery: (field, operator, value) => {
    validateOperatorAppliance(operator, type, field);

    return operatorsMap[operator](field.field, value);
  },
};

export default type;
