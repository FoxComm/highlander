/* @flow */

/**
 * Module for fetching customer groups predefined group-template
 */

import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import Api from 'lib/api';

type State = TTemplates;

const _fetchTemplates = createAsyncActions('fetchCustomerGroupsTemplates', () => Api.get('/groups/templates'));

export const fetchTemplates = _fetchTemplates.perform;

const initialState: State = [];

const reducer = createReducer({
  [_fetchTemplates.succeeded]: (state: State, response: TTemplates) => response,
}, initialState);

export default reducer;
