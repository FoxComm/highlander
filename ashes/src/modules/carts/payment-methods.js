import _ from 'lodash';
import Api from 'lib/api';
import stripe from 'lib/stripe';
import { createAction, createReducer } from 'redux-act';
import { post } from 'lib/search';
import { getBillingAddress } from 'lib/credit-card-utils';
import { toQuery } from '../../elastic/common';
import { createAsyncActions } from '@foxcomm/wings';

import { selectCreditCard } from './details';

type CreditCard = {
  isDefault: boolean,
  cardNumber: string,
  holderName: string,
  cvv: string,
  expMonth: number,
  expYear: number,
  addressId: number,
};

const _createAction = (description, ...args) => {
  return createAction('CART_PAYMENT_METHOD_' + description, ...args);
};

const setError = _createAction('ERROR');

export function createAndAddOrderCreditCardPayment(orderRefNum, creditCard, customerId) {
  return (dispatch, getState) => {
    const ccPayload = {
      isDefault: creditCard.isDefault,
      cardNumber: creditCard.cardNumber,
      holderName: creditCard.holderName,
      cvv: creditCard.cvv,
      expMonth: creditCard.expMonth,
      expYear: creditCard.expYear,
    };

    return stripe.addCreditCard(ccPayload, getBillingAddress(getState, customerId, creditCard.addressId), false)
      .then(creditCard => Api.post(`/customers/${customerId}/payment-methods/credit-cards`, creditCard))
      .then(res => dispatch(selectCreditCard(orderRefNum, res.id)))
      .catch(err => dispatch(setError(err)));
  };
}

export function editCreditCardPayment(orderRefNum, creditCard, customerId) {
  const ccPayload = {
    isDefault: creditCard.isDefault,
    holderName: creditCard.holderName,
    expMonth: creditCard.expMonth,
    expYear: creditCard.expYear,
    addressId: _.get(creditCard, 'address.id', creditCard.addressId),
  };

  return dispatch => {
    return Api.patch(`/customers/${customerId}/payment-methods/credit-cards/${creditCard.id}`, ccPayload)
      .then(res => dispatch(selectCreditCard(orderRefNum, res.id)))
      .catch(err => dispatch(setError(err)));
  };
}

const _giftCardSearch = createAsyncActions(
  'orders/giftCards',
  code => {
    const filters = [{
      term: 'code',
      operator: 'eq',
      value: {
        type: 'term',
        // for alternate approach for case-insensitive search see https://gist.github.com/mtyaka/2006966
        value: code.toUpperCase(),
      },
    }];

    return post('gift_cards_search_view/_search', toQuery(filters));
  }
);

export const giftCardSearch = _giftCardSearch.perform;

const initialState = {
  isAdding: false,
  isEditing: false,
  isFetching: false,
  isUpdating: false,
  isSearchingGiftCards: false,
  giftCards: [],
};

const reducer = createReducer({
  [_giftCardSearch.succeeded]: (state, payload) => {
    return {
      ...state,
      giftCards: payload.result,
    };
  },
  [setError]: (state, err) => {
    const errMessage = _.get(err, ['response', 'body', 'errors', 0], 'Bad Request');
    return {
      ...state,
      err: errMessage,
    };
  },
}, initialState);

export default reducer;
