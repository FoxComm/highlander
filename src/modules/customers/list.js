import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'customers', 
  searchTerms,
  'customers_search_view/_search',
  'customersScope'
);

export {
  reducer as default,
  actions
};
