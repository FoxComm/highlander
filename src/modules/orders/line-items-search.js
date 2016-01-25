import makeQuickSearch from '../quick-search';
import searchTerms from './search-terms';

const emptyFilter = {};
const emptySearch = "";
const { reducer, actions } = makeQuickSearch('line-items', emptyFilters, emptySearch);

export {
  reducer as default,
  actions
};
