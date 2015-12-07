import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const {
  reducer,
  actions: { deleteSearchFilter, goBack, submitFilter }
} = makeLiveSearch('ORDERS', searchTerms);

export default reducer;

export {
  deleteSearchFilter,
  goBack,
  submitFilter
};
