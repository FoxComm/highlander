import makeLiveSearch from 'modules/live-search';

const searchTerms = [
  {
    title: 'Customer',
    type: 'object',
    options: [
      {
        title: 'Name',
        type: 'string',
        term: 'name'
      }, {
        title: 'Email',
        type: 'string',
        term: 'email'
      }
    ]
  }
];

const { reducer, actions } = makeLiveSearch(
  'customerGroups.details.customers',
  searchTerms,
  'customers_search_view',
  'customersScope',
  {
    skipInitialFetch: true,
    initialState: { sortBy: '-joinedAt' }
  }
);

export {
  reducer as default,
  actions
};
