
// data source module

import _ from 'lodash';
import { assoc, update, merge, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import Api from '../lib/api';
import { haveType } from './state-helpers';

const _createAction = (description, ...args) => {
  return createAction('ADDRESSES_' + description, ...args);
};

export const startDeletingAddress = _createAction('START_DELETING');
export const stopDeletingAddress = _createAction('STOP_DELETING');

// API actions
const failAddress = _createAction('FAILED', (customerId, err) => [customerId, err]);
const updateAddress = _createAction('UPDATE', (customerId, addressId, data) => [customerId, addressId, data]);
const failFetchAddress = _createAction('FAILED_FETCH', (customerId, err) => [customerId, err]);
const receivedAddresses = _createAction('RECEIVED', (customerId, addresses) => [customerId, addresses]);
const requestAddresses = _createAction('REQUEST');
const addressCreated = _createAction('CREATED', (customerId, entity) => [customerId, entity]);
const removeAddress = _createAction('REMOVE', (customerId, addressId) => [customerId, addressId]);

export function fetchAddresses(customerId) {
  return dispatch => {
    dispatch(requestAddresses(customerId));

    return Api.get(`/customers/${customerId}/addresses`)
      .then(addresses => dispatch(receivedAddresses(customerId, addresses)))
      .catch(err => dispatch(failFetchAddress(customerId, err)));
  };
}

export function createAddress(customerId, data) {
  return dispatch => {
    return Api.post(`/customers/${customerId}/addresses`, data)
      .then(address => dispatch(addressCreated(customerId, address)) && address)
      .catch(err => dispatch(failAddress(customerId, err)));
  };
}

export function patchAddress(customerId, addressId, data) {
  return dispatch => {
    return Api.patch(`/customers/${customerId}/addresses/${addressId}`, data)
      .then(address => dispatch(updateAddress(customerId, addressId, address)) && address)
      .catch(err => dispatch(failAddress(customerId, err)));
  };
}

export function deleteAddress(customerId, addressId) {
  return dispatch => {
    dispatch(stopDeletingAddress(customerId));
    return Api.delete(`customers/${customerId}/addresses/${addressId}`)
      .then(ok => dispatch(removeAddress(customerId, addressId)))
      .catch(err => dispatch(failAddress(customerId, err)));
  };
}

export function setAddressDefault(customerId, addressId, isDefault) {
  return dispatch => {
    return Api.post(`customers/${customerId}/addresses/${addressId}/default`, {
        isDefault
      })
      .then(ok => dispatch(updateAddress(customerId, addressId, {isDefault})))
      .catch(err => dispatch(failAddress(customerId, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [requestAddresses]: (state, customerId) => {
    return assoc(state, [customerId, 'isFetching'], true);
  },
  [receivedAddresses]: (state, [customerId, payload]) => {
    const addresses = _.get(payload, 'result', []);

    return update(state, customerId, merge, {isFetching: false, addresses});
  },
  [updateAddress]: (state, [customerId, addressId, data]) => {
    return update(state, [customerId, 'addresses'], addresses => {
      const index = _.findIndex(addresses, {id: addressId});

      return update(addresses, index, merge, data);
    });
  },
  [failFetchAddress]: (state, [customerId, err]) => {
    console.error(err);
    return update(state, customerId, merge, {isFetching: false, err});
  },
  [failAddress]: (state, [customerId, err]) => {
    console.error(err);
    return assoc(state, customerId, 'err', err);
  },
  [removeAddress]: (state, [customerId, addressId]) => {
    return update(state, [customerId, 'addresses'], _.reject, id => addressId);
  },
  [startDeletingAddress]: (state, addressId) => {
    return assoc(state, 'deletingId', addressId);
  },
  [stopDeletingAddress]: state => {
    return dissoc(state, 'deletingId');
  }
}, initialState);

export default reducer;
