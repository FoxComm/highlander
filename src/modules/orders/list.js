import makePagination from '../pagination';
import makeLiveSearch from '../live-search';
import { createAction, createReducer } from 'redux-act';
import searchTerms from './search-terms';

const liveSearch = makeLiveSearch('ORDERS', searchTerms);
const {
  deleteSearchFilter,
  goBack,
  submitFilter,
  updateSearch
} = liveSearch.actions;

const {reducer, fetch} = makePagination('/orders', 'ORDERS', liveSearch.reducer);

export default reducer;

export {
  fetch,
  deleteSearchFilter,
  goBack,
  submitFilter,
  updateSearch,
};
