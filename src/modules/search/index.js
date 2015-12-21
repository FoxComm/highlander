//
// Search Module
//
// The set of actions and reducers that need to be exposed to effectively
// manage connecting to ElasticSearch.
//
// NOTE: At some point, this may get merged back into the Pagination Module, but
// for now this will be separate to develop a clean interface.
//

import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { assoc, get } from 'sprout-data';
import { post } from '../../lib/search';

export const searchRequest = createAction('SEARCH_REQUEST');
export const searchSuccess = createAction('SEARCH_SUCCESS');
export const searchFailure = createAction('SEARCH_FAILURE');

export function fetchSearch(url) {
  return dispatch => {
    dispatch(searchRequest());
    return post(url)
      .then(
        res => dispatch(searchSuccess(res)),
        err => dispatch(searchFailure(err, fetchSearch))
      );
  };
}

const initialState = {
  isFetching: false,
  results: []
};

const reducer = createReducer({
  [searchRequest]: (state) => {
    return assoc(state, 'isFetching', true);
  },
  [searchSuccess]: (state, res) => {
    const hits = get(res, 'hits', []);
    const results = _.map(hits, hit => {
      return get(hit, '_source', {});
    });

    return assoc(state,
      'isFetching', false,
      'results', results
    );
  },
  [searchFailure]: (state, [err, source]) => {
    if (source === fetch) {
      console.error(err);
      return assoc(state, 'isFetching', false);
    }

    return state;
  }
}, initialState);

export default reducer;
