/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

export type State = {
  catalog: ?any,
};

const initialState: State = {
  catalog: null,
};

const _createCatalog = createAsyncActions(
  'createCatalog',
  (payload: any) => Api.post('/catalogs', payload)
);

export const createCatalog = _createCatalog.perform;

const reducer = createReducer({
  [_createCatalog.succeeded]: (state, response) => {
    return { ...state, catalog: response };
  },
}, initialState);

export default reducer;
                              
