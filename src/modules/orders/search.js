import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const {
  reducer,
  actions: { cloneSearch, deleteSearchFilter, goBack, selectSavedSearch, submitFilter }
} = makeLiveSearch('ORDERS', searchTerms);

export default reducer;

export {
  cloneSearch,
  deleteSearchFilter,
  goBack,
  selectSavedSearch,
  submitFilter
};
