import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const {
  reducer,
  actions: { deleteSearchFilter, goBack, selectSavedSearch, submitFilter }
} = makeLiveSearch('ORDERS', searchTerms);

export default reducer;

export {
  deleteSearchFilter,
  goBack,
  selectSavedSearch,
  submitFilter
};
