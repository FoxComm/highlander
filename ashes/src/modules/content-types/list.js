import makeLiveSearch from '../live-search';

const searchTerms = [
  {
    title: 'Promotion : ID',
    type: 'identifier',
    term: 'id'
  },
  {
    title: 'Promotion : Name',
    type: 'string',
    term: 'promotionName'
  },
  {
    title: 'Promotion : Storefront Name',
    type: 'string',
    term: 'storefrontName'
  },
  {
    title: 'Promotion : Apply Type',
    type: 'enum',
    term: 'applyType',
    suggestions: [
      { display: 'Coupon', value: 'coupon' },
      { display: 'Auto', value: 'auto' },
    ],
  },
  {
    title: 'Promotion : Date/Time Created',
    type: 'date',
    term: 'createdAt'
  },
  {
    title: 'Promotion : Is Archived',
    type: 'exists',
    term: 'archivedAt',
    suggestions: [
      { display: 'Yes', operator: 'exists' },
      { display: 'No', operator: 'missing' },
    ],
  },
];

const { reducer, actions } = makeLiveSearch(
  'promotions.list',
  searchTerms,
  'promotions_search_view/_search',
  'promotionsScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export {
  reducer as default,
  actions
};
