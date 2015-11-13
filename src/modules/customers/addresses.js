
import _ from 'lodash';
import { assoc, update, merge, dissoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import Api from '../../lib/api';
import { haveType } from '../state-helpers';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_ADDRESSES_' + description, ...args);
};

// ui state actions
export const startAddingAddress = _createAction('START_ADDING');
export const stopAddingAddress = _createAction('STOP_ADDING');

export const startEditingAddress = _createAction('START_EDITING', (customerId, addressId) => [customerId, addressId]);
export const stopEditingAddress = _createAction('STOP_EDITING', (customerId, addressId) => [customerId, addressId]);

export const startDeletingAddress = _createAction('START_DELETING', (customerId, addressId) => [customerId, addressId]);
export const stopDeletingAddress = _createAction('STOP_DELETING');

// API actions
const failAddress = _createAction('FAILED', (customerId, err) => [customerId, err]);
const failFetchAddress = _createAction('FAILED_FETCH', (customerId, err) => [customerId, err]);
const receivedCustomerAddresses = _createAction('RECEIVED', (customerId, addresses) => [customerId, addresses]);
const requestCustomerAddresses = _createAction('REQUEST');
const addressCreated = _createAction('CREATED', (customerId, entity) => [customerId, entity]);
const removeAddress = _createAction('REMOVE', (customerId, addressId) => [customerId, addressId]);

export function fetchAddresses(customerId) {
  return dispatch => {
    dispatch(requestCustomerAddresses(customerId));

    Api.get(`/customers/${customerId}/addresses`)
      .then(addresses => dispatch(receivedCustomerAddresses(customerId, addresses)))
      .catch(err => dispatch(failFetchAddress(customerId, err)));
  };
}

export function createAddress(customerId, data) {
  return dispatch => {
    Api.post(`/customers/${customerId}/addresses`, data)
      .then(address => dispatch(addressCreated(customerId, address)))
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

const initialState = {};

const reducer = createReducer({
  [startAddingAddress]: (state, customerId) => {
    return assoc(state, [customerId, 'isAdding'], true);
  },
  [stopAddingAddress]: (state, customerId) => {
    return dissoc(state, [customerId, 'isAdding']);
  },
  [startEditingAddress]: (state, [customerId, addressId]) => {
    return update(state, [customerId, 'editingIds'], (ids = []) => {
      return [...ids, addressId];
    });
  },
  [stopEditingAddress]: (state, [customerId, addressId]) => {
    return update(state, [customerId, 'editingIds'], _.without, addressId);
  },
  [requestCustomerAddresses]: (state, customerId) => {
    return assoc(state, [customerId, 'isFetching'], true);
  },
  [receivedCustomerAddresses]: (state, [customerId, payload]) => {
    const addresses = _.get(payload, 'result', []);

    return update(state, customerId, merge, {isFetching: false, addresses});
  },
  [failFetchAddress]: (state, [customerId, err]) => {
    console.error(err);
    return update(state, customerId, merge, {isFetching: false, err});
  },
  [failAddress]: (state, [customerId, err]) => {
    console.error(err);
    return assoc(state, customerId, 'err', err);
  },
  [startDeletingAddress]: (state, [customerId, addressId]) => {
    return assoc(state, [customerId, 'deletingId'], addressId);
  },
  [stopDeletingAddress]: (state, customerId) => {
    return dissoc(state, [customerId, 'deletingId']);
  },
  [removeAddress]: (state, [customerId, addressId]) => {
    return update(state, [customerId, 'addresses'], _.reject, id => addressId);
  }
}, initialState);

export default reducer;
