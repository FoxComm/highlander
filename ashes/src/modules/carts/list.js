import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';
import * as dsl from '../../elastic/dsl';
import { addNativeFilters } from '../../elastic/common';

export const rawSorts = ['customer.name', 'customer.email'];

const { reducer, actions } = makeLiveSearch(
  'carts.list',
  searchTerms,
  'carts_search_view/_search',
  'cartsScope',
  {
    processQuery: (query) => addNativeFilters(query,[dsl.existsFilter('deletedAt', 'missing')]),
    initialState: { sortBy: '-createdAt' },
    rawSorts,
  }
);

export {
  reducer as default,
  actions
};
