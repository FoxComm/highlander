import ops from '../operators';


const type = {
  name: 'Enumeration',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.oneOf]: 'is one of',
    [ops.notOneOf]: 'is not one of',
  },
};

export default type;
