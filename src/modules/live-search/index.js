
import { update } from 'sprout-data';
import makeDataInSearches from './searches-data';
import makeSearches from './searches';
import reduceReducers from 'reduce-reducers';

export default function makeLiveSearch(namespace, searchTerms, esUrl, scope) {
  const dataInSearches = makeDataInSearches(namespace, esUrl);
  const searches = makeSearches(namespace, dataInSearches.actions.fetch, searchTerms, scope);

  const reduceSelectedSearch = (state, action) => {
    return update(state,
      ['savedSearches', state.selectedSearch, 'results'], dataInSearches.reducer, action
    );
  };

  return {
    reducer: reduceReducers(searches.reducer, reduceSelectedSearch),
    actions: {
      ...dataInSearches.actions,
      ...searches.actions,
    }
  };
}
