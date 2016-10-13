/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Info = {
  id?: number;
}

type InfoResponse = {
  legal_profile: Info
}

type State = Info;

export const ACTION_FETCH = 'merchantInfoFetch';
export const ACTION_SUBMIT = 'merchantInfoSubmit';

const { perform: submit, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, (id, data) =>
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

const { perform: fetch, ...actionsFetch } = createAsyncActions(ACTION_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/legal_profile`)
);

const initialState: State = {};

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, info: InfoResponse) => info.legal_profile,
  [actionsSubmit.succeeded]: (state: State, info: InfoResponse) => ({ ...state, ...info.legal_profile }),
}, initialState);

const getInfo = (state: State) => state;

export {
  reducer as default,
  fetch,
  submit,

  /* selectors */
  getInfo,
};

