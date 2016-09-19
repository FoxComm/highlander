/* @flow */

import { createReducer } from 'redux-act';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Account = {
  id?: number;
  first_name?: string;
  last_name?: string;
  phone_number?: string;
  business_name?: string;
  description?: string;
  email_address?: string;
  password?: string;
}

export type Accounts = {
  merchant_accounts: Array<Account>;
}

type State = Accounts;

const ACTION_MERCHANT_ACCOUNT_FETCH = 'merchantAccountFetch';
const ACTION_MERCHANT_ACCOUNT_SUBMIT = 'merchantAccountSubmit';

const { perform: performSubmit, ...actionsSubmit } = createAsyncActions(ACTION_MERCHANT_ACCOUNT_SUBMIT, (id: number, data: Object) =>
  api.post(`/merchants/${id}/accounts`, { account: { ...data } })
);

const { perform: performFetch, ...actionsFetch } = createAsyncActions(ACTION_MERCHANT_ACCOUNT_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/accounts`)
);

const initialState: State = [];

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, accounts: Accounts) => accounts.merchant_accounts,
  [actionsSubmit.succeeded]: (state: State, account) => ([...state, account]),
}, initialState);

const getAccounts = (state: State) => state;
const getAccountActionNamespace = () => ACTION_MERCHANT_ACCOUNT_SUBMIT;

export {
  reducer as default,
  performFetch as fetch,
  performSubmit as submit,

  /* selectors */
  getAccounts,
  getAccountActionNamespace,
};

