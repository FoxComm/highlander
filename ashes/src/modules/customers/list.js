import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

import * as dsl from 'elastic/dsl';

const { reducer, actions } = makeLiveSearch(
  'customers.list',
  searchTerms,
  'customers_search_view',
  'customersScope',
  {
    initialState: { sortBy: '-joinedAt' },
    rawSorts: ['name', 'email'],
    extraFilters: [
      dsl.termFilter('isGuest', false)
    ]
  }
);

export {
  reducer as default,
  actions
};
