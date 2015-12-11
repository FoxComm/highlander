const criteriaOptions = {
  isBlacklisted: {
    title: 'Blacklisted status',
    type: 'bool'
  },
  //region: {
  //  title: 'Region',
  //  type: 'enum',
  //  suggestions: []
  //},
  revenue: {
    title: 'Total Sales',
    type: 'currency'
  },
  joinedAt: {
    title: 'Date Joined',
    type: 'date'
  }
};

const criteriaOperators = {
  bool: {
    eq: 'is'
  },
  date: {
    'gt': 'is after',
    'lt': 'is before',
    'gt__lt': 'between',
  },
  number: {
    'eq': 'is equal to',
    'gt': 'is greater than',
    'lt': 'is less than',
    'gt__lt': 'between'
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
