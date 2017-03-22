/* @flow */

// libs
import _ from 'lodash';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { searchGroups } from 'elastic/customer-groups';

const _suggestGroups = createAsyncActions('suggestGroups', searchGroups);

export const suggestGroups = (excludeGroups: Array<number> = []) => _suggestGroups.perform.bind(null, excludeGroups);

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
