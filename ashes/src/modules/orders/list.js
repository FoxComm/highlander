import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'orders.list',
  searchTerms,
  'orders_search_view',
  'ordersScope',
  {
    initialState: { sortBy: '-placedAt' },
    rawSorts: ['customer.name', 'customer.email'],
  }
);

export {
  reducer as default,
  actions
};
