import ops from '../operators';


const type = {
  name: 'Number',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.less]: 'is less than',
    [ops.greater]: 'is greater than',
    [ops.between]: 'is between',
  },
};

export default type;
