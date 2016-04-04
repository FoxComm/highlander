
import makeLiveSearch from '../live-search';

const searchTerms = [];

const { reducer, actions } = makeLiveSearch(
  'promotions.list',
  searchTerms,
  'promotions_search_view/_search',
  'promotionsScope',
  {
    initialState: { sortBy: '-placedAt' }
  }
);

export {
  reducer as default,
  actions
};
