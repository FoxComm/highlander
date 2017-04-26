import _ from 'lodash';
import { createAction, createReducer } from 'redux-act';
import { post } from 'lib/search';
import * as dsl from 'elastic/dsl';
import { createAsyncActions } from '@foxcomm/wings';

const _taxonomySuggest = createAsyncActions(
  'taxonomySuggest',
  (value) => {
    return post('taxonomies_search_view/_search?size=100', dsl.query({
      bool: {
        should: [
          dsl.matchQuery('name', {
            query: value,
            operator: 'and',
          })
        ],
        minimum_should_match: 1
      },
    }));
  }
);

const resetSuggestedTaxonomies = createAction('PRODUCTS_RESET_SUGGESTED_TAXONOMIES');

export function suggestItems(value) {
  return (dispatch) => {
    return dispatch(_taxonomySuggest.perform(value));
  };
}

const initialState = {
  taxonomies: [],
};

const reducer = createReducer({
  [_taxonomySuggest.succeeded]: (state, response) => {
    return {
      taxonomies: _.get(response, 'result', []),
    };
  },
  [resetSuggestedTaxonomies]: state => {
    return {
      taxonomies: [],
    };
  },
}, initialState);

export default reducer;
