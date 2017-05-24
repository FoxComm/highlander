// data source module
import _ from 'lodash';
import { assoc, update, merge } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';

import Api from '../../lib/api';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_ADDRESSES_' + description, ...args);
};

// API actions
const failAddress = _createAction('FAILED', (customerId, err) => [customerId, err]);
const updateAddress = _createAction('UPDATE', (customerId, addressId, data) => [customerId, addressId, data]);
const failFetchAddress = _createAction('FAILED_FETCH', (customerId, err) => [customerId, err]);
const receivedAddresses = _createAction('RECEIVED', (customerId, addresses) => [customerId, addresses]);
const requestAddresses = _createAction('REQUEST');
const addressCreated = _createAction('CREATED', (customerId, entity) => [customerId, entity]);
const removeAddress = _createAction('REMOVE', (customerId, addressId) => [customerId, addressId]);
const resetDefaultFlags = _createAction('RESET_DEFAULTS');

export function fetchAddresses(customerId) {
  return dispatch => {
    dispatch(requestAddresses(customerId));

    return Api.get(`/customers/${customerId}/addresses`)
      .then(addresses => dispatch(receivedAddresses(customerId, addresses)));
  };
}

export function createAddress(customerId, data) {
  return dispatch => {
    return Api.post(`/customers/${customerId}/addresses`, data)
      .then(address => dispatch(addressCreated(customerId, address)) && address);
  };
}

export function patchAddress(customerId, addressId, data) {
  return dispatch => {
    return Api.patch(`/customers/${customerId}/addresses/${addressId}`, data)
      .then(address => dispatch(updateAddress(customerId, addressId, address)) && address);
  };
}

export function deleteAddress(customerId, addressId) {
  return dispatch => {
    return Api.delete(`customers/${customerId}/addresses/${addressId}`)
      .then(ok => dispatch(removeAddress(customerId, addressId)));
  };
}

export function setAddressDefault(customerId, addressId, isDefault) {
  return dispatch => {
    let willUpdated = null;

    if (isDefault) {
      willUpdated = Api.post(`customers/${customerId}/addresses/${addressId}/default`);
    } else {
      willUpdated = Api.delete(`customers/${customerId}/addresses/default`);
    }

    return willUpdated
      .then(ok => {
        dispatch(resetDefaultFlags(customerId));
        dispatch(updateAddress(customerId, addressId, {isDefault}));
      });
  };
}

const initialState = {};

const reducer = createReducer({
  [requestAddresses]: (state, customerId) => {
    return assoc(state, [customerId, 'isFetching'], true);
  },
  [addressCreated]: (state, [customerId, address]) => {
    return update(state, [customerId, 'addresses'], addresses => {
      return [...addresses, address];
    });
  },
  [receivedAddresses]: (state, [customerId, payload]) => {
    return assoc(state,
      [customerId, 'isFetching'], false,
      [customerId, 'addresses'], payload
    );
  },
  [updateAddress]: (state, [customerId, addressId, data]) => {
    return update(state, [customerId, 'addresses'], addresses => {
      const index = _.findIndex(addresses, {id: addressId});

      return update(addresses, index, merge, data);
    });
  },
  [resetDefaultFlags]: (state, customerId) => {
    return update(state, [customerId, 'addresses'], addresses => {
      return addresses.map(address => {
        return assoc(address, 'isDefault', false);
      });
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
    return update(state, [customerId, 'addresses'], _.reject, ({id}) => addressId == id);
  }
}, initialState);

export default reducer;
