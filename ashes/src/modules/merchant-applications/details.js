/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import { createAsyncActions } from '@foxcomm/wings';

import type { MerchantApplication, BusinessProfile, SocialProfile } from 'paragons/merchant-application';

type State = {
  application: ?MerchantApplication,
  businessProfile: ?BusinessProfile,
  socialProfile: ?SocialProfile,
};

const initialState = {
  application: null,
};

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


const _getRealSocialProfile = createAsyncActions(
  'getRealSocialProfile',
  (id: number) => Api.get(`/mkt/merchants/${id}/social_profile`)
);

const _updateSocialProfile = createAsyncActions(
  'updateSocialProfile',
  (id: number, data: Object) => Api.patch(`/mkt/merchants/${id}/social_profile`, data)
);

export const fetchApplication = _getApplication.perform;
export const fetchBusinessProfile = _getBusinessProfile.perform;
export const fetchSocialProfile = _getSocialProfile.perform;
export const approveApplication = _approveApplication.perform;
export const updateSocialProfile = _updateSocialProfile.perform;
export const fetchRealSocialProfile = _getRealSocialProfile.perform;

function applicationSucceeded(state: State, payload: Object): State {
  const application = payload.merchant_application || payload;
  console.log('astasrt');
  return { ...state, application };
}

function businessProfileSucceeded(state: State, payload: Object): State {
  const businessProfile = payload.business_profile || payload;
  return { ...state, businessProfile };
}

function socialProfileSucceeded(state: State, payload: Object): State {
  const socialProfile = payload.social_profile || payload;
  return { ...state, socialProfile };
}

const reducer = createReducer({
  [_getApplication.succeeded]: applicationSucceeded,
  [_approveApplication.succeeded]: applicationSucceeded,
  [_getBusinessProfile.succeeded]: businessProfileSucceeded,
  [_getSocialProfile.succeeded]: socialProfileSucceeded,
  [_updateSocialProfile.succeeded]: socialProfileSucceeded,
  [_getRealSocialProfile.succeeded]: socialProfileSucceeded,
}, initialState);

export default reducer;
