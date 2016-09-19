/* @flow */

import { createReducer } from 'redux-act';

import createAsyncActions from './async-utils';

import api from '../lib/api';

type Account = {
  id?: number;
}

type State = Account;

const ACTION_MERCHANT_ACCOUNT_SUBMIT = 'merchantAccountSubmit';

const { perform, ...actions } = createAsyncActions(ACTION_MERCHANT_ACCOUNT_SUBMIT, (id, data) =>
  api.post(`/merchants/${id}/accounts`, { account: { ...data } })
);

const initialState: State = {};

const reducer = createReducer({
  [actions.succeeded]: (state: State, account) => ({
    ...state,
    ...account,
  }),
}, initialState);

const getAccountId = (state: State) => state.id;
const getAccountActionNamespace = () => ACTION_MERCHANT_ACCOUNT_SUBMIT;

export {
  reducer as default,
  perform as submit,

  /* selectors */
  getAccountId,
  getAccountActionNamespace,
};

