import ops from '../operators';


const type = {
  name: 'Number',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.less]: 'is before',
    [ops.greater]: 'is after',
    [ops.between]: 'is between',
  },
};

export default type;
