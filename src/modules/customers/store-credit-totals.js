import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';

const receivedTotal = createAction('SC_TRANSACTIONS_RECEIVED_TOTAL', (customerId, payload) => [customerId, payload]);
const setError = createAction('SC_TRANSACTIONS_ERROR', (customerId, err) => [customerId, err]);

function storeCreditTotalUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit/totals`;
}

export function fetchTotals(customerId) {
  return dispatch => {
    return Api.get(storeCreditTotalUrl(customerId))
      .then(
        json => {
          dispatch(receivedTotal(customerId, json));
        },
        err => dispatch(setError(customerId, err))
      );
  };
}

const initialState = {};

const moduleReducer = createReducer({
  [receivedTotal]: (state, [customerId, payload]) => {
    return assoc(state, [customerId, 'totals'], payload);
  },
  [setError]: (state, [customerId, err]) => {
    console.error(err);

    return state;
  }
}, initialState);

export default moduleReducer;
