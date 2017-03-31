// @flow

// libs
import get from 'lodash/get';
import { createReducer } from 'redux-act';
import { createAsyncActions } from '@foxcomm/wings';
import { searchTaxonomies } from 'elastic/taxonomy';

////////////////////////////////////////////////////////////////////////////////
// Internal async actions.
////////////////////////////////////////////////////////////////////////////////

const _fetch = createAsyncActions('fetchTaxonomies', searchTaxonomies);

////////////////////////////////////////////////////////////////////////////////
// Public actions.
////////////////////////////////////////////////////////////////////////////////

export const fetch = _fetch.perform;

////////////////////////////////////////////////////////////////////////////////
// Reducer.
////////////////////////////////////////////////////////////////////////////////

const initialState = [];

const reducer = createReducer({
  [_fetch.succeeded]: (state, response) => get(response, 'result', []),
}, initialState);

export default reducer;
