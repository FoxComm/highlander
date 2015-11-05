'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

const receiveCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_RECEIVE', (id, cards) => [id, cards]);
const requestCustomerCreditCards = createAction('CUSTOMER_CREDIT_CARDS_REQUEST');

export function fetchCreditCards(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    Api.get(`/customers/${id}/payment-methods/credit-cards`)
      .then(cards => dispatch(receiveCustomerCreditCards(id, cards)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

const initialState = {};

const reducer = createReducer({
  [requestCustomerCreditCards]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: true
      }
    };
  },
  [receiveCustomerCreditCards]: (state, [id, payload]) => {
    const cards = _.get(payload, 'result', []);
    return {
      ...state,
      [id]: {
        ...state[id],
        cards,
        isFetching: false
      }
    };
  }
}, initialState);

export default reducer;
