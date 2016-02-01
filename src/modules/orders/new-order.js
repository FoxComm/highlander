import _ from 'lodash';
import Api from '../../lib/api';
import { combineReducers } from 'redux';
import { createAction, createReducer } from 'redux-act';
import { get, assoc } from 'sprout-data';
import makeQuickSearch from '../quick-search';

const emptyFilters = [];
const emptyPhrase = '';
const quickSearch = makeQuickSearch(
  'order_customers',
  'customers_search_view/_search',
  emptyFilters,
  emptyPhrase
);

function suggestCustomers(phrase) {
  return quickSearch.actions.doSearch(phrase);
}

const createOrderStart = createAction('NEW_ORDER_CREATE_ORDER_START');
const createOrderSuccess = createAction('NEW_ORDER_CREATE_ORDER_SUCCESS');
const createOrderFailure = createAction('NEW_ORDER_CREATE_ORDER_FAILURE');

function createOrder(customer, isGuest = false) {
  const payload = isGuest
    ? { email: customer.email }
    : { customerId: customer.id };
  
  return dispatch => {
    dispatch(createOrderStart());
    return Api.post('/orders', payload)
      .then(
        res => dispatch(createOrderSuccess(res)),
        err => dispatch(createOrderFailure(err))
      );
  };
}

const initialState = {
  isCreating: false,
  cart: {},
};

const orderReducer = createReducer({
  [createOrderStart]: (state) => {
    return assoc(state, 'isCreating', true);
  },
  [createOrderSuccess]: (state, payload) => {
    return assoc(state,
      'isCreating', false,
      'cart', payload
    );
  },
  [createOrderFailure]: (state, err) => {
    console.error(err);
    return assoc(state, 'isCreating', false);
  },
}, initialState);

const reducer = combineReducers({
  order: orderReducer,
  customers: quickSearch.reducer,
});

export {
  reducer as default,
  suggestCustomers,
  createOrder,
};
