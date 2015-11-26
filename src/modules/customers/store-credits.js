import _ from 'lodash';
import { assoc, update, merge, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import Api from '../../lib/api';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_STORE_CREDITS_' + description, ...args);
};

const requestStoreCredits = _createAction('REQUEST');
const receiveStoreCredits = _createAction('RECEIVE');
const failStoreCredits = _createAction("FAIL");

const initialState = {
  isFetching: false,
  storeCredits: []
};

function storeCreditsUrl(customerId) {
  return `/customers/${customerId}/payment-methods/store-credit`;
}

export function fetchStoreCredits(customerId) {
  return dispatch => {
    dispatch(requestStoreCredits());
    return Api.get(storeCreditsUrl(customerId))
      .then(storeCredits => {
        dispatch(receiveStoreCredits(storeCredits));
      })
      .catch(err => {
        dispatch(failStoreCredits(err));
      });
  };
}

const reducer = createReducer({
  [requestStoreCredits]: state => {
    return {
      ...state,
      isFetching: true
    };
  },
  [receiveStoreCredits]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      storeCredits: payload
    };
  },
  [failStoreCredits]: (state, err) => {
    console.error(err);

    return {
      ...state,
      isFetching: false
    };
  }
}, initialState);

export default reducer;
