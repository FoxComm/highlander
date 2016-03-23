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
};

export default type;
