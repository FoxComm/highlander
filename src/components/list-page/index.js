
import _ from 'lodash';

import ListPage from './list-page';
import ListPageContainer from './list-page-container';
import SearchableList from './searchable-list';

export function selectCountFromLiveSearch(liveSearchState) {
  return state => {
    const {selectedSearch, savedSearches} = liveSearchState(state);

    return {
      entitiesCount: _.get(savedSearches, [selectedSearch, 'results', 'total'])
    };
  };
}

export {
  ListPage,
  ListPageContainer,
  SearchableList,
};
