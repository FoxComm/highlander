/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

import type { OriginIntegration } from 'paragons/origin-integration';

type State = {
  originIntegration: ?OriginIntegration,
};

const initialState = {
  originIntegration: null,
};

const _getOriginIntegration = createAsyncActions(
  'getOriginIntegration',
  (id: number) => Api.get(`/mkt/users/${id}/origin_integrations`)
);

const _createOriginIntegration = createAsyncActions(
  'createOriginIntegrtion',
  (id: number, data: OriginIntegration) => Api.post(`/mkt/users/${id}/origin_integrations`, data)
);

const _updateOriginIntegration = createAsyncActions(
  'updateOriginIntegration',
  (id: number, data: OriginIntegration) => Api.patch(`/mkt/users/${id}/origin_integrations`, data)
);

export const fetchOriginIntegration = _getOriginIntegration.perform;
export const createOriginIntegration = _createOriginIntegration.perform;
export const updateOriginIntegration = _updateOriginIntegration.perform;

function originIntegrationSucceeded(state: State, payload: Object): State {
  const originIntegration = payload.origin_integration || payload;
  return { ...state, originIntegration };
}

const reducer = createReducer({
  [_getOriginIntegration.succeeded]: originIntegrationSucceeded,
  [_createOriginIntegration.succeeded]: originIntegrationSucceeded,
  [_updateOriginIntegration.succeeded]: originIntegrationSucceeded,
}, initialState);

export default reducer;
