/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

type Profile = {
  id?: number;
}

type State = Profile;

const ACTION_SUBMIT = 'merchantInfoSubmit';

const { perform, ...actions } = createAsyncActions(ACTION_SUBMIT, (id, data) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/legal_profile`, { legal_profile: { ...data } })
      .then((profile: Info) =>
        api.post(`/merchants/${id}/addresses`, { merchant_address: { ...data } })
          .then(() => resolve(profile))
          .catch(err => reject(new SubmissionError(err.response.data.errors)))
      )
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
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

