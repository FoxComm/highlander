import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'orders.list',
  searchTerms,
  'orders_search_view/_search',
  'ordersScope',
  {
    initialState: { sortBy: '-placedAt' }
  }
);

export {
  reducer as default,
  actions
};
