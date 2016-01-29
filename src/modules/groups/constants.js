const criteriaOptions = {
  isBlacklisted: {
    title: 'Account is blacklisted?',
    type: 'bool'
  },
  isGuest: {
    title: 'Account is guest?',
    type: 'bool'
  },
  isDisabled: {
    title: 'Account is active?',
    type: 'bool_inverted'
  },
  ranking: {
    title: 'Rank',
    type: 'number'
  },
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
  bool_inverted: {
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
