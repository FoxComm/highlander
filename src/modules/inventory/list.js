import makeLiveSearch from '../live-search';

const searchTerms = [];

const { reducer, actions } = makeLiveSearch(
  'inventory.list',
  searchTerms,
  'inventory_search_view/_search',
  'inventoryScope'
);

export {
  reducer as default,
  actions
};
