import makeLiveSearch from '../../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'customerGroups.dynamic.list',
  searchTerms,
  'customers_search_view/_search',
  'customersScope',
  { skipInitialFetch: true }
);

export {
  reducer as default,
  actions
};
