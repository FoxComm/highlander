/* @flow */

import { createReducer } from 'redux-act';

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
  id?: number;
  reference_number?: string;
  name?: string;
  business_name?: string;
  email_address?: string;
  description?: string;
  merchant?: Merchant;
}

type ApplicationResponse = {
  merchant_application: ApplicationResponse;
}

type State = Application;

const ACTION_MERCHANT_APPLICATION_FETCH = 'merchantApplicationFetch';
const ACTION_MERCHANT_APPLICATION_SUBMIT = 'merchantApplicationSubmit';

const { perform: performSubmit, ...actionsSubmit } = createAsyncActions(ACTION_MERCHANT_APPLICATION_SUBMIT, data =>
  new Promise((resolve, reject) =>
    api.post('/merchant_applications', { merchant_application: { ...data } })
      .then((application: Application) =>
        Promise.all([
          api.post(`/merchant_applications/${application.id}/business_profile`, { business_profile: { ...data } }),
          api.post(`/merchant_applications/${application.id}/social_profile`, { social_profile: { ...data } }),
        ])
          .then(() => resolve(application))
          .catch(() => reject())
      )
  )
);

const { perform: performFetch, ...actionsFetch } = createAsyncActions(ACTION_MERCHANT_APPLICATION_FETCH, reference =>
  api.get(`/merchant_applications/by_ref/${reference}`)
);

const initialState: State = {
  state: 'editing',
};

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
const getApplicationActionNamespace = () => ACTION_MERCHANT_APPLICATION_SUBMIT;
const getApplicationFetchActionNamespace = () => ACTION_MERCHANT_APPLICATION_FETCH;

export {
  reducer as default,
  performSubmit as submit,
  performFetch as fetch,

  /* selectors */
  getApplication,
  getApplicationActionNamespace,
  getApplicationFetchActionNamespace,
};

