
import { update } from 'sprout-data';
import makeDataInSearches from './searches-data';
import makeSearches from './searches';
import reduceReducers from 'reduce-reducers';

export default function makeLiveSearch(namespace, searchTerms, esUrl, scope, options) {
  const dataInSearches = makeDataInSearches(namespace, esUrl, options);
  const searches = makeSearches(namespace, dataInSearches.actions.fetch, searchTerms, scope, options);

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
