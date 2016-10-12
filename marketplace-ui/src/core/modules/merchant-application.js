/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Merchant = {
  id: number;
  state: string;
  name: string;
  description: string;
}

export type Application = {
  state: string;
  id: number;
  reference_number: string;
  business_name: string;
  email_address: string;
  description: string;
  merchant?: Merchant;
}

type ApplicationResponse = {
  merchant_application: ApplicationResponse;
}

type State = {} | Application;

export const ACTION_FETCH = 'merchantApplicationFetch';
export const ACTION_SUBMIT = 'merchantApplicationSubmit';

const { perform: submit, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, data =>
  new Promise((resolve, reject) =>
    api.post('/merchant_applications_full', { merchant_application: { ...data } })
      .then((application: Application) => resolve(application))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const { perform: fetch, clearErrors, ...actionsFetch } = createAsyncActions(ACTION_FETCH, reference =>
  api.get(`/merchant_applications/by_ref/${reference}`)
);

const initialState: State = {};

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, application: ApplicationResponse) => ({
    ...state,
    ...application.merchant_application,
  }),
  [actionsSubmit.succeeded]: (state: State, application: Application) => ({
    ...state,
    ...application,
  }),
}, initialState);

const getApplication = (state: State) => state;
const getApplicationApproved = (state: State) => state.state === 'approved';

export {
  reducer as default,
  submit,
  fetch,
  clearErrors,

  /* selectors */
  getApplication,
  getApplicationApproved,
};

