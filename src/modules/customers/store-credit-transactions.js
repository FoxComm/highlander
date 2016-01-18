
import _ from 'lodash';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import makePagination from '../pagination/structured-store';
import Api from '../../lib/api';

const dataPath = storeCreditId => [storeCreditId];

const receivedTotal = createAction('SC_TRANSACTIONS_RECEIVED_TOTAL', (customerId, payload) => [customerId, payload]);
const setError = createAction('SC_TRANSACTIONS_ERROR', (customerId, err) => [customerId, err]);

const { makeActions, makeReducer } = makePagination('STORECREDIT_TRANSACTIONS', dataPath);

function storeCreditTransactionsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit/transactions`;
}

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

const moduleReducer = createReducer({
  [receivedTotal]: (state, [customerId, payload]) => {
    return assoc(state, [customerId, 'totals'], payload);
  },
  [setError]: (state, [customerId, err]) => {
    console.log(err);

    return state;
  }
});

const { fetch } = makeActions(storeCreditTransactionsUrl);
const reducer = makeReducer(moduleReducer);

const fetchStoreCreditTransactions = (customerId, params = {}) => fetch(customerId, params);

export {
  reducer as default,
  fetchStoreCreditTransactions
};
