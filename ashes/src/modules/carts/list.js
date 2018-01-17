import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';
import * as dsl from '../../elastic/dsl';
import { addNativeFilters, addShouldFilters } from '../../elastic/common';

const { reducer, actions } = makeLiveSearch(
  'carts.list',
  searchTerms,
  'carts_search_view',
  'cartsScope',
  {
    processQuery: (query) => {
      query = addNativeFilters(query, [dsl.existsFilter('deletedAt', 'missing')]);
      query = addShouldFilters(query, [
        dsl.rangeFilter('lineItemCount', { gt: 0 }),
        dsl.nestedExistsFilter('customer.email', 'exists'),
      ]);

      return query;
    },
    initialState: { sortBy: '-createdAt' },
    rawSorts: ['customer.name', 'customer.email'],
  }
);

export {
  reducer as default,
  actions
};
