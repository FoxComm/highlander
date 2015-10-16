'use strict';

// state for gift card adding form

import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';


export const changeFormData = createAction('GIFT_CARDS_NEW_CHANGE_FORM', (name, value) => ({name, value}));
export const suggestCustomers = createAction('GIFT_CARDS_NEW_SUGGEST_CUSTOMERS');
export const suggestUsers = createAction('GIFT_CARDS_NEW_SUGGEST_USERS');
export const addCustomer = createAction('GIFT_CARDS_NEW_ADD_CUSTOMER');
export const removeCustomer = createAction('GIFT_CARDS_NEW_REMOVE_CUSTOMER');
export const addUser = createAction('GIFT_CARDS_NEW_ADD_USER');
export const removeUser = createAction('GIFT_CARDS_NEW_REMOVE_USER');

const balanceToText = balance => (balance / 100).toFixed(2);
const textToBalance = value => value * 100;

const initialState = {
  customers: [],
  users: [],
  balance: 100,
  balanceText: balanceToText(100),
  originType: 'Appeasement',
  sendToCustomer: false,
  emailCSV: false,
  types: {
    Appeasement: [],
    Marketing: ['One', 'Two']
  }
};

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
  [suggestCustomers]: (state, customersQuery) => {
    return {
      ...state,
      customersQuery
    };
  },
  [suggestUsers]: (state, usersQuery) => {
    return {
      ...state,
      usersQuery
    };
  },
  [addCustomer]: (state, customer) => {
    return {
      ...state,
      customers: _.uniq([...state.customers, customer], customer => customer.id)
    };
  },
  [removeCustomer]: (state, id) => {
    return {
      ...state,
      customers: _.reject(state.customers, customer => customer.id == id)
    };
  },
  [addUser]: (state, user) => {
    return {
      ...state,
      users: _.uniq([...state.users, user], user => user.id)
    };
  },
  [removeUser]: (state, id) => {
    return {
      ...state,
      users: _.reject(state.users, users => users.id == id)
    };
  }
}, initialState);

export default reducer;