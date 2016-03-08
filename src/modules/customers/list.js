import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'customers.list',
  searchTerms,
  'customers_search_view/_search',
  'customersScope',
  {
    initialState: { sortBy: '-joinedAt' }
  }
);

export {
  reducer as default,
  actions
};
