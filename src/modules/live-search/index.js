import { update } from 'sprout-data';
import makeDataInSearches from './searches-data';
import makeSearches from './searches';
import reduceReducers from 'reduce-reducers';

export default function makeLiveSearch(namespace, searchTerms, esUrl, scope, options) {
  const dataInSearches = makeDataInSearches(namespace, esUrl, options);

  const fetchActions = {
    fetch: dataInSearches.actions.fetch,
    searchStart: dataInSearches.actions.searchStart,
    searchSuccess: dataInSearches.actions.searchSuccess,
    searchFailure: dataInSearches.actions.searchFailure
  };

  const searches = makeSearches(namespace, fetchActions, searchTerms, scope, options);

  const reduceSelectedSearch = (state, action) => {
    return update(state,
      ['savedSearches', state.selectedSearch, 'results'], dataInSearches.reducer, action
    );
  };

  const reducer = reduceReducers(
    searches.reducer,
    reduceSelectedSearch,
    dataInSearches.rootReducer
  );

  return {
    reducer,
    actions: {
      ...dataInSearches.actions,
      ...searches.actions,
    }
  };
}
