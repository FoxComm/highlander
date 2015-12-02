const criteriaOptions = {
  blacklisted: {
    title: 'Blacklisted status',
    type: 'bool'
  },
  region: {
    title: 'Region',
    type: 'enum',
    suggestions: []
  },
  revenue: {
    title: 'Total Sales',
    type: 'currency'
  },
  date_joined: {
    title: 'Date Joined',
    type: 'date'
  }
};

const criteriaOperators = {
  bool: {
    eq: 'is'
  },
  date: {
    '>': 'is after',
    '<': 'is before',
    'between': 'between'
  },
  number: {
    '=': 'is equal to',
    '>': 'is greater than',
    '<': 'is less than'
  },
  enum: {
    'has': 'is one of',
    'not_has': 'is not one of'
  }
};
criteriaOperators['currency'] = criteriaOperators['number'];

export {
  criteriaOptions,
  criteriaOperators
};
