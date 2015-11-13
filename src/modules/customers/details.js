import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';
import { assoc, update, deepMerge } from 'sprout-data';


const receiveCustomer = createAction('CUSTOMER_RECEIVE', (id, customer) => [id, customer]);
const failCustomer = createAction('CUSTOMER_FAIL', (id, err, source) => [id, err, source]);
const requestCustomer = createAction('CUSTOMER_REQUEST');
const updateCustomer = createAction('CUSTOMER_UPDATED', (id, customer) => [id, customer]);
const receiveCustomerAdresses = createAction('CUSTOMER_ADDRESSES_RECEIVE', (id, addresses) => [id, addresses]);

const requestCustomerAdresses = createAction('CUSTOMER_ADDRESSES_REQUEST');


// status
const submitToggleDisableStatus = createAction('CUSTOMER_SUBMIT_DISABLE_STATUS');
const receivedDisableStatus = createAction('CUSTOMER_RECEIVED_DISABLE_STATUS', (id, customer) => [id, customer]);

const submitToggleBlacklisted = createAction('CUSTOMER_SUBMIT_BLACKLISTED');
const receivedBlacklisted = createAction('CUSTOMER_RECEIVED_BLACKLISTED', (id, customer) => [id, customer]);

const failChangeStatus = createAction('CUSTOMER_FAIL_CHANGE_STATUS', (id, err) => [id, err]);

export const startDisablingCustomer = createAction('CUSTOMER_START_DISABLING');
export const stopDisablingCustomer = createAction('CUSTOMER_STOP_DISABLING');

export const startBlacklistCustomer = createAction('CUSTOMER_START_BLACKLIST');
export const stopBlacklistCustomer = createAction('CUSTOMER_STOP_BLACKLIST');


export function fetchCustomer(id) {
  return dispatch => {
    dispatch(requestCustomer(id));
    Api.get(`/customers/${id}`)
      .then(customer => dispatch(receiveCustomer(id, customer)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

export function editCustomer(id, data) {
  return dispatch => {
    Api.patch(`/customers/${id}`, data)
      .then(customer => dispatch(updateCustomer(id, customer)))
      .catch(err => dispatch(failCustomer(id, err, editCustomer)));
  };
}

export function fetchAddresses(id) {
  return dispatch => {
    dispatch(requestCustomerAdresses(id));

    Api.get(`/customers/${id}/addresses`)
      .then(addresses => dispatch(receiveCustomerAdresses(id, addresses)))
      .catch(err => dispatch(failCustomer(id, err, fetchCustomer)));
  };
}

export function toggleDisableStatus(id, isDisabled) {
  return dispatch => {
    dispatch(stopDisablingCustomer());
    dispatch(submitToggleDisableStatus(id));

    Api.post(`/customers/${id}/disable`, {disabled: isDisabled})
      .then(customer => dispatch(receivedDisableStatus(id, customer)))
      .catch(err => dispatch(failChangeStatus(id, err)));
  };
}

export function toggleBlacklisted(id, isBlacklisted) {
  return dispatch => {
    dispatch(stopBlacklistCustomer());
    dispatch(submitToggleBlacklisted(id));

    Api.post(`/customers/${id}/blacklist`, {blacklisted: isBlacklisted})
      .then(customer => dispatch(receivedBlacklisted(id, customer)))
      .catch(err => dispatch(failChangeStatus(id, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [requestCustomer]: (entries, id) => {
    return assoc(entries,
      [id, 'isFetching'], true,
      [id, 'err'], null
    );
  },
  [receiveCustomer]: (state, [id, details]) => {
    return assoc(state,
      [id, 'err'], null,
      [id, 'isFetching'], false,
      [id, 'details'], haveType(details, 'customer')
    );
  },
  [failCustomer]: (state, [id, err, source]) => {
    console.error(err);

    return assoc(state,
      [id, 'err'], err,
      [id, 'isFetching'], false
    );
  },
  [updateCustomer]: (state, [id, details]) => {
    return assoc(state,
      [id, 'details'], details,
      [id, 'err'], null
    );
  },
  [requestCustomerAdresses]: (state, id) => {
    return assoc(state, [id, 'isFetchingAddresses'], true);
  },
  [submitToggleDisableStatus]: (state, id) => {
    return assoc(state, [id, 'isFetchingStatus'], true);
  },
  [submitToggleBlacklisted]: (state, id) => {
    return assoc(state, [id, 'isFetchingStatus'], true);
  },
  [receivedDisableStatus]: (state, [id, customer]) => {
    return update(state, id, deepMerge, {
      isFetchingStatus: false,
      details: {disabled: customer.disabled}});
  },
  [receivedBlacklisted]: (state, [id, customer]) => {
    return update(state, id, deepMerge, {
      isFetchingStatus: false,
      details: {blacklisted: customer.blacklisted}});
  },
  [failChangeStatus]: (state, [id, err]) => {
    console.error(err);
    return assoc(state, [id, 'isFetchingStatus'], false);
  },
  [startDisablingCustomer]: (state) => {
    return assoc(state, 'isDisablingStarted', true);
  },
  [stopDisablingCustomer]: (state) => {
    return assoc(state, 'isDisablingStarted', false);
  },
  [startBlacklistCustomer]: (state) => {
    return assoc(state, 'isBlacklistedStarted', true);
  },
  [stopBlacklistCustomer]: (state) => {
    return assoc(state, 'isBlacklistedStarted', false);
  },
  [receiveCustomerAdresses]: (state, [id, payload]) => {
    const addresses = _.get(payload, 'result', []);

    return assoc(state,
      [id, 'isFetchingAddresses'], false,
      [id, 'addresses'], addresses
    );
  }
}, initialState);

export default reducer;
