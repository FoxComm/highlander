import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Coupon : Code',
    type: 'string',
    term: 'codes',
  },
  {
    title: 'Coupon : Date/Time Created',
    type: 'date',
    term: 'createdAt',
  },
  {
    title: 'Coupon : Is Archived',
    type: 'exists',
    term: 'archivedAt',
    suggestions: [
      { display: 'Yes', operator: 'exists' },
      { display: 'No', operator: 'missing' },
    ],
  },
];

const { reducer, actions } = makeLiveSearch(
  'coupons.list',
  searchTerms,
  'coupons_search_view',
  'couponsScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export {
  reducer as default,
  actions
};
