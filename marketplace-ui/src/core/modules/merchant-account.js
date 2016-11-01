/* @flow */

import findIndex from 'lodash/findIndex';
import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Account = {
  merchant_account: {
    id?: number;
    first_name?: string;
    last_name?: string;
    phone_number?: string;
    business_name?: string;
    description?: string;
    email_address?: string;
    password?: string;
    stripe_account_id?: string;
  }
}

export type Accounts = Array<Account>;

type AccountsResponse = {
  merchant_accounts: Accounts
}

type State = Accounts;

export const ACTION_FETCH = 'merchantAccountFetch';
export const ACTION_SUBMIT = 'merchantAccountSubmit';
export const ACTION_SUBMIT_BUSINESS = 'merchantAccountBusinessSubmit';

const { perform: submitAccount, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, (id: number, data: Object) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/admin_accounts`, { account: { ...data } })
      .then((account: Account) => resolve(account))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const { perform: submitBusiness, ...actionsSubmitBusiness } = createAsyncActions(ACTION_SUBMIT_BUSINESS, (id: number, data: Object) =>
  new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/legal_profile`, { legal_profile: { ...data } })
      .then((account: Account) =>
        api.post(`/merchants/${id}/addresses`, { merchant_address: { ...data } })
          .then(() => resolve(account))
          .catch(err => reject(new SubmissionError(err.response.data.errors)))
      )
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  )
);

const { perform: fetch, ...actionsFetch } = createAsyncActions(ACTION_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/accounts`)
);

const initialState: State = [];

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, accounts: AccountsResponse) => accounts.merchant_accounts,
  [actionsSubmit.succeeded]: (state: State, account) => ([...state, account]),
  [actionsSubmitBusiness.succeeded]: (state: State, account: Account) => {
    const index = findIndex(state, (acc: Account) => acc.merchant_account.id === account.merchant_account.id);

    state.splice(index, 1, account);

    return state;
  },
}, initialState);

const getAccounts = (state: State) => state;

export {
  reducer as default,
  fetch,
  submitAccount,
  submitBusiness,

  /* selectors */
  getAccounts,
};

