/* @flow */

import { createReducer } from 'redux-act';

import createAsyncActions from './async-utils';

import api from '../lib/api';

type Profile = {
  id?: number;
}

type State = Profile;

const ACTION_SUBMIT = 'merchantInfoSubmit';

const { perform, ...actions } = createAsyncActions(ACTION_SUBMIT, (id, data) =>
  api.post(`/merchants/${id}/business_profile`, { business_profile: { ...data } })
);

const initialState: State = {};

const reducer = createReducer({
  [actions.succeeded]: (state: State, info) => ({
    ...state,
    ...info,
  }),
}, initialState);

const getInfoActionNamespace = () => ACTION_SUBMIT;

export {
  reducer as default,
  perform as submit,

  /* selectors */
  getInfoActionNamespace,
};

