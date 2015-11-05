'use strict';

import _ from 'lodash';
import { assoc, update, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import Api from '../../lib/api';
import { haveType } from '../state-helpers';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_ADDRESSES_' + description, ...args);
};

const failAddress = _createAction('FAILED', (entity, err) => [entity, err]);
const failFetchAddress = _createAction('FAILED_FETCH', (entity, err) => [entity, err]);
const receivedCustomerAddresses = _createAction('RECEIVED', (id, addresses) => [id, addresses]);
const requestCustomerAddresses = _createAction('REQUEST');
const addressCreated = _createAction('CREATED', (id, entity) => [id, entity]);


export function fetchAddresses(id) {
  return dispatch => {
    dispatch(requestCustomerAddresses(id));

    Api.get(`/customers/${id}/addresses`)
      .then(addresses => dispatch(receivedCustomerAddresses(id, addresses)))
      .catch(err => dispatch(failFetchAddress(id, err)));
  };
}

export function createAddress(id,  data) {
  return dispatch => {
    Api.post(`/customers/${id}/addresses`, data)
      .then(address => dispatch(addressCreated(id, address)))
      .catch(err => dispatch(failAddress(id, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [requestCustomerAddresses]: (state, id) => {
    return assoc(state, [id, 'isFetching'], true);
  },
  [receivedCustomerAddresses]: (state, [id, payload]) => {
    const addresses = _.get(payload, 'result', []);
    return update(state, id, merge, {isFetching: false, addresses});
  },
  [failFetchAddress]: (state, [id, err]) => {
    console.error(err);
    return update(state, id, merge, { isFetching: false, err});
  },
  [failAddress]: (state, [id, err]) => {
    console.error(err);
    return assoc(state, id, 'err', err);
  }
}, initialState);

export default reducer;