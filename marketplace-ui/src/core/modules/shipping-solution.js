/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Shipping = {
  id?: number;
}

type State = Shipping;

export const ACTION_FETCH = 'shippingSolutionFetch';
export const ACTION_SUBMIT = 'shippingSolutionSubmit';

const { perform: fetch, ...actionsFetch } = createAsyncActions(ACTION_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/shipping`)
);

const { perform: submit, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, (id, data) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/shipping`, { shipping: { ...data } })
      .then((shipping: Shipping) => resolve(shipping))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const initialState: State = {};

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, shipping: Shipping) => shipping,
  [actionsSubmit.succeeded]: (state: State, shipping: Shipping) => shipping,
}, initialState);

const getShipping = (state: State) => state;

export {
  reducer as default,
  fetch,
  submit,

  /* selectors */
  getShipping,
};
