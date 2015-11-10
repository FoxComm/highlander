import _ from 'lodash';
import Api from '../../lib/api';
import { createAction, createReducer } from 'redux-act';
import { haveType } from '../state-helpers';

const _createAction = (description, ...args) => {
  return createAction('CUSTOMER_CREDIT_CARD_' + description, ...args);
};

// State change reducers
export const newCustomerCreditCard = _createAction('NEW');
export const closeNewCustomerCreditCard = _createAction('NEW_CLOSE');
export const changeNewCustomerCreditCardFormData = _createAction('NEW_CHANGE_FORM',
                                                                 (id, name, value) => [id, name, value]);
export const editCustomerCreditCard = _createAction('EDIT',
                                                    (customerId, cardId) => [customerId, cardId]);
export const closeEditCustomerCreditCard = _createAction('EDIT_CLOSE',
                                                         (customerId, cardId) => [customerId, cardId]);
export const changeEditCustomerCreditCardFormData = _createAction('EDIT_CHANGE_FORM',
                                                                  (id, name, value) => [id, name, value]);
export const deleteCustomerCreditCard = _createAction('DELETE',
                                                      (customerId, cardId) => [customerId, cardId]);
export const closeDeleteCustomerCreditCard = _createAction('DELETE_CLOSE');

// API reducers
const receiveCustomerCreditCards = _createAction('RECEIVE',
                                                 (id, cards) => [id, cards]);
const requestCustomerCreditCards = _createAction('REQUEST');
const failCustomerCreditCards = _createAction('FAIL',
                                              (id, err, source) => [id, err]);

function fetchForCustomer(id, dispatch) {
  Api.get(`/customers/${id}/payment-methods/credit-cards`)
    .then(cards => dispatch(receiveCustomerCreditCards(id, cards)))
    .catch(err => dispatch(failCustomerCreditCards(id, err)));
}

export function fetchCreditCards(id) {
  return dispatch => {
    dispatch(requestCustomerCreditCards(id));

    fetchForCustomer(id, dispatch);
  };
}

export function createCreditCard(id) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(id));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const cardData = _.get(cards, [id, 'newCreditCard'], {});

    Api.post(`/customers/${id}/payment-methods/credit-cards`, cardData)
      .then(() => {
        dispatch(closeNewCustomerCreditCard(id));
        fetchForCustomer(id, dispatch);
      })
      .catch(err => dispatch(failCustomerCreditCards(id, err)));

  };
}

export function confirmCreditCardDeletion(id) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(id));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const creditCardId = _.get(cards, [id, 'deletingId']);
    Api.delete(`/customers/${id}/payment-methods/credit-cards/${creditCardId}`)
      .then(() => {
        dispatch(closeDeleteCustomerCreditCard(id));
        fetchForCustomer(id, dispatch);
      })
      .catch(err => dispatch(failCustomerCreditCards(id, err)));
  };
}

export function saveCreditCard(id) {
  return (dispatch, getState) => {
    dispatch(requestCustomerCreditCards(id));

    const cards = _.get(getState(), 'customers.creditCards', {});
    const creditCardId = _.get(cards, [id, 'editingId']);
    const form = _.get(cards, [id, 'editingCreditCard']);
    Api.patch(`/customers/${id}/payment-methods/credit-cards/${creditCardId}`, form)
      .then(() => {
        dispatch(closeDeleteCustomerCreditCard(id));
        fetchForCustomer(id, dispatch);
      })
      .catch(err => dispatch(failCustomerCreditCards(id, err)));
  };
}

const initialState = {};

const reducer = createReducer({
  [newCustomerCreditCard]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        newCreditCard: {
          isDefault: false,
          holderName: null,
          number: null,
          cvv: null,
          expMonth: null,
          expYear: null,
          addressId: 1
        }
      }
    };
  },
  [closeNewCustomerCreditCard]: (state, id) => {
    const { newCreditCard, ...restState } = state[id];
    return {
      ...state,
      [id]: {
        ...restState
      }
    };
  },
  [changeNewCustomerCreditCardFormData]: (state, [id, name, value]) => {
    const newCreditCard = _.get(state, [id, 'newCreditCard']);
    const newState = {
      ...state,
      [id]: {
        ...state[id],
        newCreditCard: {
          ...newCreditCard,
          [name]: value
        }
      }
    };

    return newState;
  },
  [editCustomerCreditCard]: (state, [customerId, cardId]) => {
    const cards = _.get(state, [customerId, 'cards'], []);
    const creditCard = _.find(cards, (card) => { return (cardId === card.id); });
    const {holderName, expMonth, expYear, isDefault} = creditCard;
    return {
      ...state,
      [customerId]: {
        ...state[customerId],
        editingId: cardId,
        editingCreditCard: {
          holderName,
          expMonth,
          expYear,
          isDefault
        }
      }
    };
  },
  [changeEditCustomerCreditCardFormData]: (state, [id, name, value]) => {
    const editingCreditCard = _.get(state, [id, 'editingCreditCard']);
    const newState = {
      ...state,
      [id]: {
        ...state[id],
        editingCreditCard: {
          ...editingCreditCard,
          [name]: value
        }
      }
    };

    return newState;
  },
  [closeEditCustomerCreditCard]: (state, [customerId, cardId]) => {
    return {
      ...state,
      [customerId]: {
        ...state[customerId],
        editingId: null,
        editingCreditCard: null
      }
    };
  },
  [deleteCustomerCreditCard]: (state, [customerId, cardId]) => {
    return {
      ...state,
      [customerId]: {
        ...state[customerId],
        deletingId: cardId
      }
    };
  },
  [closeDeleteCustomerCreditCard]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        deletingId: null
      }
    };
  },
  [requestCustomerCreditCards]: (state, id) => {
    return {
      ...state,
      [id]: {
        ...state[id],
        isFetching: true
      }
    };
  },
  [receiveCustomerCreditCards]: (state, [id, payload]) => {
    const cards = _.get(payload, 'result', []);
    return {
      ...state,
      [id]: {
        ...state[id],
        cards,
        isFetching: false
      }
    };
  },
  [failCustomerCreditCards]: (state, [id, err]) => {
    console.error(err);

    return {
      ...state,
      [id]: {
        ...state[id],
        err,
        isFetching: false
      }
    };
  },
}, initialState);

export default reducer;
