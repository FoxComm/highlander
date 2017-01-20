import * as dsl from 'elastic/dsl';
import { addNativeFilters } from 'elastic/common';
import makeLiveSearch from '../live-search';

const { reducer, actions } = makeLiveSearch(
  'customerGroups.list',
  [],
  'customer_groups_search_view/_search',
  'customerGroupsScope',
  {
    processQuery: (query) => addNativeFilters(query,[dsl.existsFilter('deletedAt', 'missing')]),
    initialState: { sortBy: '-createdAt' },
  }
);

export {
  reducer as default,
  actions,
};
