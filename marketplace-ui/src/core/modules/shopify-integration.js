/* @flow */

import { pick } from 'lodash';
import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import request from '../lib/request';
import api from '../lib/api';

export type ShopifyIntegration = {
  id?: number,
};

type State = ShopifyIntegration;

export const ACTION_SUBMIT = 'shopifyIntegrationSubmit';

const { perform: submit, ...submitActions } = createAsyncActions(ACTION_SUBMIT, (merchantId, data) => {
  return new Promise((resolve, reject) =>
    api.post(`/merchants/${merchantId}/shopify_integration`, { shopify_integration: data })
      .then((integration: ShopifyIntegration) => resolve(integration))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  );
});

const initialState: State = {};

const reducer = createReducer({
  [submitActions.succeeded]: (state: State, integration: ShopifyIntegration) => integration,
}, initialState);

const getShopify = (state: State): ShopifyIntegration => state;

export {
  reducer as default,
  submit,

  /* selectors */
  getShopify,
};