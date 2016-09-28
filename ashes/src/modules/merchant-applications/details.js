/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from 'modules/async-utils';

import type { MerchantApplication, BusinessProfile, SocialProfile } from 'paragons/merchant-application';

type State = {
  application: ?MerchantApplication,
  businessProfile: ?BusinessProfile,
  socialProfile: ?SocialProfile,
};

const initialState = {
  application: null,
}

const _getApplication = createAsyncActions(
  'getApplication',
  (id: number) => Api.get(`/mkt/merchant_applications/${id}`)
);

const _getBusinessProfile = createAsyncActions(
  'getBusinessProfile',
  (id: number) => Api.get(`/mkt/merchant_applications/${id}/business_profile`)
);

const _getSocialProfile = createAsyncActions(
  'getSocialProfile',
  (id: number) => Api.get(`/mkt/merchant_applications/${id}/social_profile`)
);

const _approveApplication = createAsyncActions(
  'approveApplication',
  (id: number) => Api.post(`/mkt/merchants/activate_application/${id}`)
);

export const fetchApplication = _getApplication.perform;
export const fetchBusinessProfile = _getBusinessProfile.perform;
export const fetchSocialProfile = _getSocialProfile.perform;
export const approveApplication = _approveApplication.perform;

function applicationSucceeded(state: State, payload: Object): State {
  const application = payload.merchant_application || payload;
  console.log('astasrt');
  return { ...state, application };
}

function businessProfileSucceeded(state: State, payload: Object): State {
  const application = payload.business_profile || payload;
  return { ...state, application };
}

function socialProfileSucceeded(state: State, payload: Object): State {
  const application = payload.social_profile || payload;
  return { ...state, application };
}

const reducer = createReducer({
  [_getApplication.succeeded]: applicationSucceeded,
  [_approveApplication.succeeded]: applicationSucceeded,
  [_getBusinessProfile.succeeded]: (state) => businessProfileSucceeded,
  [_getSocialProfile.succeeded]: (state) => socialProfileSucceeded,
}, initialState);

export default reducer;
