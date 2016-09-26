/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from 'modules/async-utils';

import type { MerchantApplication } from 'paragons/merchant-application';

type State = {
  application: ?MerchantApplication,
};

const initialState = {
  application: null,
}

const _getApplication = createAsyncActions(
  'getApplication',
  (id: number) => Api.get(`/mkt/merchant_applications/${id}`)
);

const _updateApplication = createAsyncActions(
  'updateApplication',
  (id: number) => Api.patch(`/mkt/merchant_applications/${id}`)
);

export const fetchApplication = _getApplication.perform;
export const updateApplication = _updateApplication.perform;

function applicationSucceeded(state: State, payload: Object): State {
  const application = payload.merchant_application || payload;
  return { ...state, application };
}

const reducer = createReducer({
  [_getApplication.succeeded]: applicationSucceeded,
  [_updateApplication.succeeded]: applicationSucceeded,
}, initialState);

export default reducer;
