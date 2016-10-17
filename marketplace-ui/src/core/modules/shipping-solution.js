/* @flow */

import { endsWith } from 'lodash';
import { createReducer } from 'redux-act';
import { SubmissionError } from 'redux-form';

import createAsyncActions from './async-utils';

import api from '../lib/api';

export type Shipping = {
  id?: number;
}

type ShippingResponse = {
  shipping_solutions: Array<Shipping>;
}

type State = Shipping;

export const ACTION_FETCH = 'shippingSolutionFetch';
export const ACTION_SUBMIT = 'shippingSolutionSubmit';

const formatPrice = value => Number(value) * 1e2;

const { perform: fetch, ...actionsFetch } = createAsyncActions(ACTION_FETCH, merchantId =>
  api.get(`/merchants/${merchantId}/shipping_solutions`)
);

const { perform: submit, ...actionsSubmit } = createAsyncActions(ACTION_SUBMIT, (id, data) => {
  const payload = Object.keys(data).reduce((methods, field) => {
    if (!endsWith(field.toLowerCase(), 'price') && data[field]) {
      methods.push({
        carrier_name: field,
        price: formatPrice(data[`${field}Price`]),
      });
    }

    return methods;
  }, []);

  return new Promise((resolve, reject) =>
    api.post(`/merchants/${id}/shipping_solutions`, { shipping_solutions: payload })
      .then((shipping: Shipping) => resolve(shipping))
      .catch(err => reject(new SubmissionError(err.response.data.errors)))
  );
});

const initialState: State = {};

const reducer = createReducer({
  [actionsFetch.succeeded]: (state: State, shipping: ShippingResponse) => shipping.shipping_solutions,
  [actionsSubmit.succeeded]: (state: State, shipping: ShippingResponse) => shipping.shipping_solutions,
}, initialState);

const getShipping = (state: State) => state;

export {
  reducer as default,
  fetch,
  submit,

  /* selectors */
  getShipping,
};
