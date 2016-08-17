import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const { reducer, actions } = makeLiveSearch(
  'carts.list',
  searchTerms,
  'carts_search_view/_search',
  'cartsScope',
  {
    initialState: { sortBy: '-placedAt' },
    rawSorts: ['customer.name', 'customer.email']
  }
);

export {
  reducer as default,
  actions
};
