
/* @flow weak */

// state for gift card adding form

import _ from 'lodash';
import Api from '../../lib/api';
import { combineReducers } from 'redux';
import { createAction, createReducer } from 'redux-act';
import { assoc } from 'sprout-data';


const _createAction = (desc, ...args) => createAction(`GIFT_CARDS_NEW_${desc}`, ...args);

export const changeFormData = _createAction('CHANGE_FORM', (name, value) => ({name, value}));
export const addCustomers = _createAction('ADD_CUSTOMERS');
export const removeCustomer = _createAction('REMOVE_CUSTOMER');
export const changeQuantity = _createAction('CHANGE_QUANTITY');
export const resetForm = _createAction('RESET_FORM');
const setError = _createAction('ERROR');
const setTypes = _createAction('SET_TYPES');

import makeQuickSearch from '../quick-search';

const emptyFilters = [];
const emptyPhrase = '';
const quickSearch = makeQuickSearch(
  'giftCards.adding.suggestedCustomers',
  'customers_search_view/_search',
  emptyFilters,
  emptyPhrase
);

export function suggestCustomers(phrase) {
  const filters = [
    {
      term: 'name',
      operator: 'eq',
      value: {
        type: 'string',
        value: phrase,
      },
    },
    {
      term: 'email',
      operator: 'eq',
      value: {
        type: 'string',
        value: phrase,
      },
    },
  ];

  return quickSearch.actions.fetch('', filters, {atLeastOne: true});
}

const initialState = {
  customers: [],
  users: [],
  balance: 0,
  quantity: 1,
  reasonId: null,
  originType: 'csrAppeasement',
  sendToCustomer: false,
  emailCSV: false,
  types: [],
  balances: [1000, 2500, 5000, 10000, 20000],
};

export function fetchTypes() {
  return dispatch => {
    Api.get(`/public/gift-cards/types`)
      .then(
        types => dispatch(setTypes(types)),
        err => dispatch(setError(err))
      );
  };
}

const giftCardReducer = createReducer({
  [changeFormData]: (state, {name, value}) => {
    const newState = assoc(state, name, value);
    switch (name) {
      case 'sendToCustomer':
        const quantity = value ? newState.customers.length : Math.max(newState.customers.length, 1);
        return assoc(newState, 'quantity', quantity);
      default:
        return newState;
    }
  },
  [addCustomers]: (state, customers) => {
    const newCustomers = _.uniq([...state.customers, ...customers], customer => customer.id);
    return {
      ...state,
      customers: newCustomers,
      quantity: state.sendToCustomer ? newCustomers.length : state.quantity
    };
  },
  [removeCustomer]: (state, id) => {
    const newCustomers = _.reject(state.customers, customer => customer.id == id);
    return {
      ...state,
      customers: newCustomers,
      quantity: state.sendToCustomer ? newCustomers.length : state.quantity
    };
  },
  [changeQuantity]: (state, amount) => {
    amount = Math.max(Number(amount) || 1, 1);

    return {
      ...state,
      quantity: amount
    };
  },
  [setTypes]: (state, types) => {
    // allow only csrAppeasement type
    const filteredTypes = _.filter(types, type => type.originType === 'csrAppeasement');
    const subTypeId = _.get(filteredTypes, [0, 'subTypes', 0, 'id']);
    return {
      ...state,
      types: filteredTypes,
      originType: 'csrAppeasement',
      subTypeId,
    };
  },
  [setError]: (state, err) => {
    console.error(err);

    return state;
  },
  [resetForm]: (state) => {
    return {
      ...initialState,
      types: state.types,
    };
  },
}, initialState);


const reducer = combineReducers({
  giftCard: giftCardReducer,
  suggestedCustomers: quickSearch.reducer
});

export default reducer;
