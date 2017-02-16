/* @flow */

import get from 'lodash/get';
import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

import type { MerchantApplication } from 'paragons/merchant-application';

type State = {
  applications: Array<MerchantApplication>,
};

const initialState = {
  applications: [],
};

const _getApplications = createAsyncActions(
  'getApplications',
  () => Api.get(`/mkt/merchant_applications`)
);

export const fetchApplications = _getApplications.perform;

const reducer = createReducer({
  [_getApplications.succeeded]: (state: State, payload: Object): State => {
	  const applications = get(payload, 'merchant_applications', payload);
	  return { ...state, applications };
	},
}, initialState);

export default reducer;
