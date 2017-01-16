import makeLiveSearch from '../live-search';

const { reducer, actions } = makeLiveSearch(
  'customerGroups.list',
  [],
  'customer_groups_search_view/_search',
  'customerGroupsScope',
  {
    initialState: { sortBy: '-createdAt' },
  }
);

export {
  reducer as default,
  actions,
};
