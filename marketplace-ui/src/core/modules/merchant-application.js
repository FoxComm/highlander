/* @flow */

import { createReducer } from 'redux-act';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Application = {
  state: string;
  id?: number;
  reference_number?: string;
  name?: string;
  business_name?: string;
  email_address?: string;
  description?: string;
}

type State = Application;

const ACTION_MERCHANT_APPLICATION_FETCH = 'merchantApplicationFetch';
const ACTION_MERCHANT_APPLICATION_SUBMIT = 'merchantApplicationSubmit';

const { perform: performSubmit, ...actionsSubmit } = createAsyncActions(ACTION_MERCHANT_APPLICATION_SUBMIT, data =>
  api.post('/merchant_applications', { merchant_application: { ...data } })
);

const { perform: performFetch, ...actionsFetch } = createAsyncActions(ACTION_MERCHANT_APPLICATION_FETCH, reference =>
  api.get(`/merchant_applications/${reference}`)
);

const initialState: State = {
  state: 'editing',
};

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, application) => ({
    ...state,
    ...application.merchant_application,
  }),
  [actionsSubmit.succeeded]: (state: State, application) => ({
    ...state,
    ...application,
  }),
}, initialState);

const getApplication = (state: State) => state;
const getApplicationActionNamespace = () => ACTION_MERCHANT_APPLICATION_SUBMIT;

export {
  reducer as default,
  performSubmit as submit,
  performFetch as fetch,

  /* selectors */
  getApplication,
  getApplicationActionNamespace,
};

