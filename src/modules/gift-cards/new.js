// state for gift card adding form

import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';

const _createAction = (desc, ...args) => createAction(`GIFT_CARDS_NEW_${desc}`, ...args);

export const changeFormData = _createAction('CHANGE_FORM', (name, value) => ({name, value}));
export const addCustomers = _createAction('ADD_CUSTOMERS');
export const removeCustomer = _createAction('REMOVE_CUSTOMER');
export const changeQuantity = _createAction('CHANGE_QUANTITY');
const setSuggestedCustomers = _createAction('SET_SUGGESTED_CUSTOMERS');
const setError = _createAction('ERROR');
const setTypes = _createAction('SET_TYPES');

const balanceToText = balance => (balance / 100).toFixed(2);
const textToBalance = value => value * 100;

export function suggestCustomers(term) {
  return dispatch => {
    return Api.get(`/customers/searchForNewOrder`, {term})
      .then(
        response => dispatch(setSuggestedCustomers(response.result)),
        err => dispatch(setSuggestedCustomers([]))
      );
  };
}

const initialState = {
  customers: [],
  suggestedCustomers: [],
  users: [],
  balance: 100,
  quantity: 1,
  balanceText: balanceToText(100),
  originType: 'Appeasement',
  sendToCustomer: false,
  emailCSV: false,
  types: [],
};

export function fetchTypes() {
  return dispatch => {
    Api.get(`/gift-cards/types`)
      .then(
        types => dispatch(setTypes(types)),
        err => dispatch(setError(err))
      );
  };
}

const reducer = createReducer({
  [changeFormData]: (state, {name, value}) => {
    const newState = {
      ...state,
      [name]: value
    };

    if (name === 'balanceText') {
      newState.balance = textToBalance(value);
    }

    if (name === 'balance') {
      newState.balanceText = balanceToText(value);
    }

    return newState;
  },
  [setSuggestedCustomers]: (state, customers) => {
    return {
      ...state,
      suggestedCustomers: customers
    };
  },
  [addCustomers]: (state, customers) => {
    return {
      ...state,
      customers: _.uniq([...state.customers, ...customers], customer => customer.id)
    };
  },
  [removeCustomer]: (state, id) => {
    return {
      ...state,
      customers: _.reject(state.customers, customer => customer.id == id)
    };
  },
  [changeQuantity]: (state, amount) => {
    amount = Number(amount);
    if (isNaN(amount)) amount = 1;
    amount = Math.max(amount, 1);

    return {
      ...state,
      quantity: amount
    };
  },
  [setTypes]: (state, types) => {
    // allow only csrAppeasement type
    const filteredTypes = _.filter(types, type => type.originType === 'csrAppeasement');
    return {
      ...state,
      types: filteredTypes,
      originType: 'csrAppeasement',
      subTypeId: filteredTypes[0].subTypes[0].id
    };
  },
  [setError]: (state, err) => {
    console.error(err);

    return state;
  }
}, initialState);

export default reducer;
