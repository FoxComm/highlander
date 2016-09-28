/* @flow */

import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

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

export type Accounts = Array<Account>;

type AccountsResponse = {
  merchant_accounts: Accounts
}

type State = Accounts;

const ACTION_FETCH = 'merchantAccountFetch';
const ACTION_SUBMIT = 'merchantAccountSubmit';

const { perform: performSubmit, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, (id: number, data: Object) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/accounts`, { account: { ...data } })
      .then((account: Account) => resolve(account))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const { perform: performFetch, ...actionsFetch } = createAsyncActions(ACTION_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/accounts`)
);

const initialState: State = [];

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, accounts: AccountsResponse) => accounts.merchant_accounts,
  [actionsSubmit.succeeded]: (state: State, account) => ([...state, account]),
}, initialState);

const getAccounts = (state: State) => state;
const getAccountActionNamespace = () => ACTION_SUBMIT;

export {
  reducer as default,
  performFetch as fetch,
  performSubmit as submit,

  /* selectors */
  getAccounts,
  getAccountActionNamespace,
};

