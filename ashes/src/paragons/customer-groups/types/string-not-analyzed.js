import ops from '../operators';


const type = {
  name: 'String',
  operators: {
    [ops.equal]: 'is',
    [ops.notEqual]: 'is not',
    [ops.oneOf]: 'is one of',
    [ops.notOneOf]: 'is not one of',
    [ops.containsNotAnalyzed]: 'contains',
    [ops.notContains]: 'does not contain',
  },
};

export default type;
