/* @flow */

import { createReducer } from 'redux-act';

import createAsyncActions from './async-utils';

import api from '../lib/api';

type Application = {
  state: string;
  id?: number;
  reference_number?: string;
  name?: string;
  business_name?: string;
  email_address?: string;
  description?: string;
}

type State = Application;

const ACTION_MERCHANT_APPLICATION_SUBMIT = 'merchantApplicationSubmit';

const { perform, ...actions } = createAsyncActions(ACTION_MERCHANT_APPLICATION_SUBMIT, data =>
  api.post('/merchant_applications', { merchant_application: { ...data } })
);

const initialState: State = {
  state: 'editing',
};

const reducer = createReducer({
  [actions.succeeded]: (state: State, application) => ({
    ...state,
    ...application,
  }),
}, initialState);

const getApplyFormActionNamespace = () => ACTION_MERCHANT_APPLICATION_SUBMIT;

export {
  reducer as default,
  perform as submit,

  /* selectors */
  getApplyFormActionNamespace,
};

