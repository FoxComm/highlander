import _ from 'lodash';
import Api from '../../lib/api';
import { assoc } from 'sprout-data';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';


const receiveCustomer = createAction('CUSTOMER_RECEIVE', (id, customer) => [id, customer]);
const failCustomer = createAction('CUSTOMER_FAIL', (id, err, source) => [id, err, source]);
const requestCustomer = createAction('CUSTOMER_REQUEST');
const updateCustomer = createAction('CUSTOMER_UPDATED', (id, customer) => [id, customer]);
const receiveCustomerAdresses = createAction('CUSTOMER_ADDRESSES_RECEIVE', (id, addresses) => [id, addresses]);

const requestCustomerAdresses = createAction('CUSTOMER_ADDRESSES_REQUEST');


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
  [receiveCustomerAdresses]: (state, [id, payload]) => {
    const addresses = _.get(payload, 'result', []);

    return assoc(state,
      [id, 'isFetchingAddresses'], false,
      [id, 'addresses'], addresses
    );
  }
}, initialState);

export default reducer;
