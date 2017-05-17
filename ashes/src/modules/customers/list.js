import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

import * as dsl from 'elastic/dsl';
export const rawSorts = ['name', 'email'];

const { reducer, actions } = makeLiveSearch(
  'customers.list',
  searchTerms,
  'customers_search_view/_search',
  'customersScope',
  {
    initialState: { sortBy: '-joinedAt' },
    rawSorts,
    extraFilters: [
      dsl.termFilter('isGuest', false)
    ]
  }
);

export {
  reducer as default,
  actions
};
