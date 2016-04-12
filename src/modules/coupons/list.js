
import makeLiveSearch from '../live-search';

const searchTerms = [];

const { reducer, actions } = makeLiveSearch(
  'coupons.list',
  searchTerms,
  'coupons_search_view/_search',
  'inventoryScope',
  {
    initialState: { sortBy: '-createdAt' }
  }
);

export {
  reducer as default,
  actions
};
