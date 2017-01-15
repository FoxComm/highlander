/* @flow */

// it's module for customer-groups/select-groups component

import { createAsyncActions } from '@foxcomm/wings';
import Api from '../../lib/api';
import { createReducer } from 'redux-act';

const _fetchCustomerGroups = createAsyncActions(
  'customerGroups',
  () => {
    return Api.get('/groups');
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
      groups: response,
    };
  }
}, initialState);

export default reducer;
