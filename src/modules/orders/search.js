import makeLiveSearch from '../live-search';
import searchTerms from './search-terms';

const {
  reducer,
  actions: {
    cloneSearch,
    deleteSearchFilter,
    editSearchNameStart,
    editSearchNameCancel,
    editSearchNameComplete,
    goBack,
    saveSearch,
    selectSavedSearch,
    submitFilter
  }
} = makeLiveSearch('ORDERS', searchTerms);

export default reducer;

export {
  cloneSearch,
  deleteSearchFilter,
  editSearchNameStart,
  editSearchNameCancel,
  editSearchNameComplete,
  goBack,
  saveSearch,
  selectSavedSearch,
  submitFilter
};
