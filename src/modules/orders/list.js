import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'orders', 
  searchTerms, 
  'orders_search_view/_search',
  'ordersScope'
);

export {
  reducer as default,
  actions
};
