import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

export const rawSorts = ['customer.name', 'customer.email'];

const { reducer, actions } = makeLiveSearch(
  'orders.list',
  searchTerms,
  'orders_search_view/_search',
  'ordersScope',
  {
    initialState: { sortBy: '-placedAt' },
    rawSorts,
  }
);

export {
  reducer as default,
  actions
};
