/* @flow */

// libs
import _ from 'lodash';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { searchGroups } from 'elastic/customer-groups';

const _suggestGroups = createAsyncActions(
  'suggestGroups',
  term => searchGroups(term)
);
export const suggestGroups = _suggestGroups.perform;

const initialState = {
  groups: [],
};

const reducer = createReducer({
  [_suggestGroups.succeeded]: (state, response) => {
    return {
      ...state,
      groups: _.isEmpty(response.result) ? [] : response.result,
    };
  },
}, initialState);

export default reducer;
