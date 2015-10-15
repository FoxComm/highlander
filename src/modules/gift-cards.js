'use strict';

import _ from 'lodash';
import Api from '../lib/api';
import { createAction, createReducer } from 'redux-act';

export const receiveGiftCards = createAction('GIFT_CARDS_RECEIVE');
export const updateGiftCards = createAction('GIFT_CARDS_UPDATE');
export const failGiftCards = createAction('GIFT_CARDS_FAIL', (err, source) => {err, source});
export const requestGiftCards = createAction('GIFT_CARDS_REQUEST');

export function fetchGiftCards() {
  return dispatch => {
    dispatch(requestGiftCards());
    return Api.get('/gift-cards')
      .then(cards => dispatch(receiveGiftCards(cards)))
      .catch(err => dispatch(failGiftCards(err)));
  };
}

function shouldFetchGiftCards(state) {
  const giftCards = state.giftCards;
  if (!giftCards) {
    return true;
  } else if (giftCards.isFetching) {
    return false;
  }
  return giftCards.didInvalidate;
}

export function fetchGiftCardsIfNeeded() {
  return (dispatch, getState) => {
    if (shouldFetchGiftCards(getState())) {
      return dispatch(fetchGiftCards());
    }
  };
}

export function fetchGiftCard(id) {
  return Api.get(`/gift-cards/${id}`)
    .then(card => updateGiftCards([card]))
    .catch(err => failGiftCards(err, fetchGiftCard));
}

export function createGiftCard(form) {
  return Api.submitForm(form)
    .then(card => updateGiftCards([card]))
    .catch(err => failGiftCards(err));
}

export function editGiftCard(id, data) {
  return Api.patch(`/gift-cards/${id}`, data)
    .then(card => updateGiftCards([card]))
    .catch(err => failGiftCards(err));
}

const initialState = {
  isFetching: false,
  didInvalidate: true,
  items: []
};

function updateItems(items, newItems) {
  return _.values({
    ..._.indexBy(items, 'id'),
    ..._.indexBy(newItems, 'id')
  });
}

const reducer = createReducer({
  [requestGiftCards]: (state) => {
    return {
      ...state,
      isFetching: true,
      didInvalidate: false
    };
  },
  [receiveGiftCards]: (state, payload) => {
    return {
      ...state,
      isFetching: false,
      didInvalidate: false,
      items: payload
    };
  },
  [updateGiftCards]: (state, payload) => {
    return {
      ...state,
      items: updateItems(state.items, payload)
    };
  },
  [failGiftCards]: (state, {err, source}) => {
    console.error(err);

    if (source === fetchGiftCards) {
      return {
        ...state,
        isFetching: false,
        didInvalidate: false
      };
    }

    return state;
  }
}, initialState);

export default reducer;
