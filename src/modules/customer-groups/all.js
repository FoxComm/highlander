/* @flow */

// it's module for customer-groups/select-groups component

import createAsyncActions from '../async-utils';
import Api from '../../lib/api';
import { createReducer } from 'redux-act';

const _fetchCustomerGroups = createAsyncActions(
  'customerGroups',
  () => {
    return Api.get('/groups?size=100');
  }
);

export const fetchCustomerGroups = _fetchCustomerGroups.perform;

const initialState = {
  groups: [],
};

const reducer = createReducer({
  [_fetchCustomerGroups.succeeded]: (state, response) => {
    return {
      ...state,
      groups: response.result,
    };
  }
}, initialState);

export default reducer;
