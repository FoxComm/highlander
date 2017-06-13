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

const _fetchCatalog = createAsyncActions(
  'fetchCatalog',
  (id: number) => Api.get(`/catalogs/${id}`)
);

const _createCatalog = createAsyncActions(
  'createCatalog',
  (payload: any) => Api.post('/catalogs', payload)
);

const _updateCatalog = createAsyncActions(
  'updateCatalog',
  (id: number, payload: any) => Api.patch(`/catalogs/${id}`, payload)
);

export const fetchCatalog = _fetchCatalog.perform;
export const createCatalog = _createCatalog.perform;
export const updateCatalog = _updateCatalog.perform;

const handleResponse = (state, response) => ({ ...state, catalog: response });

const reducer = createReducer({
  [_fetchCatalog.succeeded]: handleResponse,
  [_createCatalog.succeeded]: handleResponse,
  [_updateCatalog.succeeded]: handleResponse,
}, initialState);

export default reducer;
                              
