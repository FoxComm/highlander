/* @flow */

// it's module for customer-groups/select-groups component

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';

const _fetchCustomerGroups = createAsyncActions('fetchCustomerGroups', () => Api.get('/groups'));

export const fetchCustomerGroups = _fetchCustomerGroups.perform;

const initialState = {
  groups: [],
};

const reducer = createReducer({
  [_fetchCustomerGroups.succeeded]: (state, response) => ({ ...state, groups: response }),
}, initialState);

export default reducer;
