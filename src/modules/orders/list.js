import _ from 'lodash';
import makePagination from '../pagination';
import makeLiveSearch from '../live-search';
import { createAction, createReducer } from 'redux-act';

const liveSearch = makeLiveSearch('ORDERS');
const {
  deleteSearchFilter,
  goBack,
  submitFilter,
  updateSearch
} = liveSearch.actions;

const {
  reducer, 
  actions: {fetch, setFetchParams}
} = makePagination('/orders', 'ORDERS', liveSearch.reducer);

export default reducer;

export {
  fetch,
  setFetchParams,
  deleteSearchFilter,
  goBack,
  submitFilter,
  updateSearch
};
