'use strict';

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

const receiveCustomerAdresses = createAction('CUSTOMER_ADDRESSES_RECEIVE', (id, addresses) => [id, addresses]);
const requestCustomerAdresses = createAction('CUSTOMER_ADDRESSES_REQUEST');


export function fetchAdresses(id) {
  return dispatch => {
    dispatch(requestCustomerAdresses(id));

    Api.get(`/customers/${id}/addresses`)
      .then(addresses => dispatch(receiveCustomerAdresses(id, addresses)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

const initialState = {};

const reducer = createReducer({
  [requestCustomerAdresses]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetchingAddresses: true
      }
    };
  },
  [receiveCustomerAdresses]: (state, [id, payload]) => {
    const addresses = _.get(payload, 'result', []);
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetchingAddresses: false,
        addresses
      }
    };
  }
}, initialState);

export default reducer;