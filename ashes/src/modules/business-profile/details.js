/* @flow */

import { createReducer } from 'redux-act';
import Api from 'lib/api';
import createAsyncActions from 'modules/async-utils';

import type { BusinessProfile } from 'paragons/business-profile';

type State = {
  businessProfile: ?BusinessProfile,
};

const initialState = {
  businessProfile: {},
};

const _updateBusinessProfile = createAsyncActions(
  'updateBusinessProfile',
  (id: number, data: BusinessProfile) => Api.patch(`/merchants/${id}/legal_profile`, data)
);

export const updateBusinessProfile = _updateBusinessProfile.perform;

const reducer = createReducer({
  [_updateBusinessProfile.succeeded]: (state: State, payload: Object) => {
    const businessProfile = payload.business_profile || payload;
    return { ...state, businessProfile };
  },
}, initialState);

export default reducer;