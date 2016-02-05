import _ from 'lodash';
import { post } from '../../lib/search';
import { toQuery } from '../../elastic/common';

import makePagination, { makeFetchAction } from '../pagination/base';

// module is responsible for data in search tab

export default function makeDataInSearches(namespae, esUrl) {
  const getSelectedSearch = (state) => {
    const dataPath = [namespace, 'list', 'selectedSearch'];
    const selectedSearch = _.get(state, dataPath);
    const resultPath = [namespace, 'list', 'savedSearches', selectedSearch];
    return _.get(state, resultPath);
  };

  const {reducer, ...actions} = makePagination(namespace);

  const fetcher = ({state, getState}) => {
    let sort = null;

    if (state.sortBy) {
      const field = state.sortBy.replace('-', '');
      const sortByField = {
        [field]: {
          order: state.sortBy.charAt(0) == '-' ? 'desc': 'asc'
        }
      };
      sort = [sortByField];
    }

    const searchTerms = _.get(getSelectedSearch(getState()), 'query', []);
    const esQuery = toQuery(searchTerms);

    const jsonQuery = esQuery.toJSON();
    if (sort) {
      jsonQuery.sort = sort;
    }

    return post(esUrl, jsonQuery);
  };

  const fetch = makeFetchAction(fetcher, actions, state => getSelectedSearch(state).results);

  // for overriding updateStateAndFetch in paginateActions
  const updateStateAndFetch = (newState, ...args) => {
    return dispatch => {
      dispatch(actions.updateState(newState));
      dispatch(fetch(...args));
    };
  };

  return {
    reducer,
    actions: {
      ...actions,
      updateStateAndFetch,
    }
  };
}
